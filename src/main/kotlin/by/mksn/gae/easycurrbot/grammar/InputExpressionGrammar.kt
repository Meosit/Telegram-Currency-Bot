package by.mksn.gae.easycurrbot.grammar


import by.mksn.gae.easycurrbot.AppConfig
import by.mksn.gae.easycurrbot.entity.ExpressionType
import by.mksn.gae.easycurrbot.entity.RawInputQuery
import by.mksn.gae.easycurrbot.entity.toConfScale
import by.mksn.gae.easycurrbot.entity.toConfScaledBigDecimal
import by.mksn.gae.easycurrbot.service.ExchangeRateService
import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.grammar.token
import com.github.h0tk3y.betterParse.lexer.TokenMatch
import com.github.h0tk3y.betterParse.parser.*
import java.math.BigDecimal
import java.text.DecimalFormat


private sealed class InputKey(val currencyCode: String) {
    class Add(currencyCode: String) : InputKey(currencyCode)
    class Remove(currencyCode: String) : InputKey(currencyCode)
}


private data class EvaluatedInput(
        val expressionResult: BigDecimal,
        val baseCurrency: String,
        val keys: List<InputKey>
)


data class DivisionByZero(val zeroToken: TokenMatch) : ErrorResult()


@Suppress("PrivatePropertyName")
class InputExpressionGrammar(
        private val config: AppConfig,
        private val exchangeRateService: ExchangeRateService
) : Grammar<RawInputQuery>() {

    private val NUMBER by token(config.strings.tokenNames.number, "\\d+([.,]\\d+)?")
    private val KILO by token(config.strings.tokenNames.kilo, config.strings.kiloSpecialChar)
    private val LEFT_PAR by token(config.strings.tokenNames.leftPar, "\\(")
    private val RIGHT_PAR by token(config.strings.tokenNames.rightPar, "\\)")
    private val MULTIPLY by token(config.strings.tokenNames.multiply, "\\*")
    private val DIVIDE by token(config.strings.tokenNames.divide, "/")
    private val MINUS by token(config.strings.tokenNames.minus, "-")
    private val PLUS by token(config.strings.tokenNames.plus, "\\+")
    private val WHITESPACE by token(config.strings.tokenNames.whitespace, "\\s+", ignore = true)
    private val CURRENCY by token(config.strings.tokenNames.currency, "[A-Z]{3}")

    // parameters list
    private val currency by CURRENCY use { text }
    private val currencyKey by skip(WHITESPACE) and
            (((skip(MINUS) and parser(this::currency)) map { InputKey.Remove(it) }) or
                    ((skip(PLUS) and parser(this::currency)) map { InputKey.Add(it) }))

    private val keyChain by zeroOrMore(currencyKey)

    // simple math expression
    private val number by NUMBER and zeroOrMore(KILO) map { (num, kilos) ->
        kilos.foldRight(num.text.toConfScaledBigDecimal(config)) { _, acc -> acc * 1000.toConfScaledBigDecimal(config) }
    }

    private val term: Parser<BigDecimal> by number or
            (skip(MINUS) and parser(this::term) map { -it }) or
            (skip(LEFT_PAR) and parser(this::subSumChain) and skip(RIGHT_PAR))

    private val divMulChain by leftAssociative(term, DIVIDE or MULTIPLY use { type }) { a, op, b -> if (op == DIVIDE) a / b else a * b }
    private val subSumChain by leftAssociative(divMulChain, PLUS or MINUS use { type }) { a, op, b -> if (op == PLUS) a + b else a - b }

    // math number or expression in pars with currency modifier
    private val currenciedTerm: Parser<BigDecimal> by (divMulChain and currency map { (num, curr) -> exchangeRateService.exchangeToApiBase(num, curr) }) or
            (skip(MINUS) and parser(this::currenciedTerm) map { -it }) or
            (skip(LEFT_PAR) and parser(this::currenciedSubSumChain) and skip(RIGHT_PAR))

    // division and multiplication can be performed only with simple numbers
    private val currenciedDivMulChain by (currenciedTerm and oneOrMore((DIVIDE or MULTIPLY) and term) map { (initial, operands) ->
        operands.fold(initial) { a, (op, b) -> if (op.type == DIVIDE) a / b else a * b }
    }) or currenciedTerm

    // addition and subtraction can be performed only by currencied numbers/expressions
    private val currenciedSubSumChain by leftAssociative(currenciedDivMulChain, PLUS or MINUS use { type }) { a, op, b -> if (op == PLUS) a + b else a - b }


    // resulting parsers
    private val singleCurrencyInputParser by optional(subSumChain) and optional(currency) and keyChain map { (exprResult, exprCurrency, keys) ->
        EvaluatedInput(exprResult ?: 1.toConfScaledBigDecimal(config), exprCurrency ?: config.currencies.apiBase, keys)
    }
    private val multiCurrencyInputParser by currenciedSubSumChain and keyChain map { (exprResult, keys) ->
        EvaluatedInput(exprResult, config.currencies.apiBase, keys)
    }


    private val expressionInputParser by currenciedSubSumChain or subSumChain or (currency use { 1.toConfScaledBigDecimal(config) })
    private val fullInputParser by multiCurrencyInputParser or singleCurrencyInputParser


    private val unaryOperatorAtStartRegex = "^ - (\\d+([.,]\\d+)?( [A-Z]{3})?)".toRegex()
    private val unaryOperatorAfterBinaryRegex = "([-+*/] ?) - (\\d+([.,]\\d+)?( [A-Z]{3})?)".toRegex()
    private val zeroNumberRegex = "0+([.,]0+)?".toRegex()
    private val numberWithKiloRegex = "(\\d+([.,]\\d+)?)(k+)".toRegex()

    private val outputFormat = DecimalFormat(config.currencies.outputSumPattern)

    private fun List<TokenMatch>.toPrettyPrintExpression(): String =
            if (size == 1 && this[0].type == CURRENCY) "1" else
                this.asSequence()
                        .filter { it.type != WHITESPACE }
                        .map {
                            when (it.type) {
                                KILO -> "k"
                                MINUS -> " - "
                                PLUS -> " + "
                                CURRENCY -> " ${it.text}"
                                else -> it.text
                            }
                        }.joinToString("")
                        .replace("( - ", "(-")
                        .replace(unaryOperatorAtStartRegex) { "(-${it.groups[1]!!.value})" }
                        .replace(unaryOperatorAfterBinaryRegex) { "${it.groups[1]!!.value}(-${it.groups[2]!!.value})" }
                        .replace(numberWithKiloRegex) {
                            val base = it.groups[1]!!.value.toBigDecimal()
                            val res = it.groups[3]!!.value.foldRight(base.toConfScale(config)) { _, acc -> acc * 1000.toConfScaledBigDecimal(config) }
                            outputFormat.format(res)
                        }

    private fun List<TokenMatch>.findExpressionType() = when {
        size == 1 && first().type == CURRENCY -> ExpressionType.SINGLE_VALUE
        any { it.type == CURRENCY } -> ExpressionType.MULTI_CURRENCY_EXPR
        asSequence()
                .filterNot { it.type == NUMBER }
                .filterNot { it.type == WHITESPACE }
                .filterNot { it.type == KILO }
                .any() -> ExpressionType.SINGLE_CURRENCY_EXPR
        else -> ExpressionType.SINGLE_VALUE
    }

    override val rootParser = object : Parser<RawInputQuery> {
        override fun tryParse(tokens: Sequence<TokenMatch>): ParseResult<RawInputQuery> {
            val fullInput = try {
                fullInputParser.tryParse(tokens)
            } catch (e: ArithmeticException) {
                val zeroToken = tokens.filterNot { it.type == WHITESPACE }
                        .filter { it.type == DIVIDE || it.type == NUMBER }
                        .zipWithNext { a, b -> if (a.type == DIVIDE && b.type == NUMBER && b.text.matches(zeroNumberRegex)) b else null }
                        .filterNotNull().first()
                DivisionByZero(zeroToken)
            }

            return when (fullInput) {
                is Parsed -> {
                    when (val expressionInput = expressionInputParser.tryParse(tokens)) {
                        is Parsed -> {
                            with(fullInput.value) {
                                val exprTokens = tokens.toList().dropLast(expressionInput.remainder.count())

                                val involvedCurrencies = exprTokens.asSequence()
                                        .filter { it.type == CURRENCY }
                                        .map { it.text }
                                        .distinct()
                                        .toList()
                                        .ifEmpty { listOf(baseCurrency) }

                                // single value or expression with a single currency (e.g. "1 USD" auto exchanged to BYN while parsing) (grammar threat this as multicurrency expression)
                                if (involvedCurrencies.size == 1 && (involvedCurrencies[0] != baseCurrency || involvedCurrencies[0] == config.currencies.apiBase)) {
                                    val normalizedExprTokens = exprTokens.asSequence().filter { it.type != CURRENCY } + tokenizer.tokenize(involvedCurrencies[0])
                                    return when (val fixedExpressionInput = singleCurrencyInputParser.tryParseToEnd(normalizedExprTokens)) {
                                        is Parsed -> {
                                            val tokensWithoutCurrency = normalizedExprTokens.toList().dropLast(1)
                                            Parsed(RawInputQuery(
                                                    tokensWithoutCurrency.findExpressionType(),
                                                    tokensWithoutCurrency.toPrettyPrintExpression(),
                                                    fixedExpressionInput.value.expressionResult.toConfScale(config),
                                                    fixedExpressionInput.value.baseCurrency,
                                                    involvedCurrencies,
                                                    keys.filterIsInstance<InputKey.Add>().map { it.currencyCode },
                                                    keys.filterIsInstance<InputKey.Remove>().map { it.currencyCode }
                                            ), fullInput.remainder)
                                        }
                                        is ErrorResult -> fixedExpressionInput
                                    }
                                } else {
                                    return Parsed(RawInputQuery(
                                            exprTokens.findExpressionType(),
                                            exprTokens.toPrettyPrintExpression(),
                                            expressionResult.toConfScale(config),
                                            baseCurrency,
                                            involvedCurrencies,
                                            keys.filterIsInstance<InputKey.Add>().map { it.currencyCode },
                                            keys.filterIsInstance<InputKey.Remove>().map { it.currencyCode }
                                    ), fullInput.remainder)
                                }
                            }
                        }
                        is ErrorResult -> expressionInput
                    }
                }
                is ErrorResult -> fullInput
            }
        }
    }

    fun isCurrency(tokenMatch: TokenMatch) = tokenMatch.type == CURRENCY

}