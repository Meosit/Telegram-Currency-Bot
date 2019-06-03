package by.mksn.gae.easycurrbot.grammar


import by.mksn.gae.easycurrbot.AppConfig
import by.mksn.gae.easycurrbot.entity.ExpressionType
import by.mksn.gae.easycurrbot.entity.RawInputQuery
import by.mksn.gae.easycurrbot.service.ExchangeRateService
import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.grammar.token
import com.github.h0tk3y.betterParse.lexer.TokenMatch
import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.ParseResult
import com.github.h0tk3y.betterParse.parser.Parsed
import com.github.h0tk3y.betterParse.parser.Parser
import java.math.BigDecimal
import java.math.RoundingMode


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
    private val number by NUMBER use { text.toBigDecimal().setScale(config.currencies.internalPrecision, RoundingMode.HALF_UP) }

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
    private val currenciedDivMulChain by (currenciedTerm and oneOrMore((DIVIDE or MULTIPLY) and term) map {
        (initial, operands) -> operands.fold(initial) { a, (op, b) -> if (op.type == DIVIDE) a / b else a * b }
    }) or currenciedTerm

    // addition and subtraction can be performed only by currencied numbers/expressions
    private val currenciedSubSumChain by leftAssociative(currenciedDivMulChain, PLUS or MINUS use { type }) { a, op, b -> if (op == PLUS) a + b else a - b }


    // resulting parsers
    private val singleCurrencyInputParser by subSumChain and optional(currency) and keyChain map { (exprResult, exprCurrency, keys) ->
        EvaluatedInput(exprResult, exprCurrency ?: config.currencies.apiBase, keys)
    }
    private val multiCurrencyInputParser by currenciedSubSumChain and keyChain map { (exprResult, keys) ->
        EvaluatedInput(exprResult, config.currencies.apiBase, keys)
    }


    private val expressionInputParser by currenciedSubSumChain or subSumChain
    private val fullInputParser by multiCurrencyInputParser or singleCurrencyInputParser


    private val unaryOperatorAtStart = "^ - (\\d+([.,]\\d+)?( [A-Z]{3})?)".toRegex()
    private val unaryOperatorAfterBinary = "([-+*/] ?) - (\\d+([.,]\\d+)?( [A-Z]{3})?)".toRegex()

    private fun List<TokenMatch>.toPrettyPrintExpression(): String = this.asSequence()
            .filter { it.type != WHITESPACE }
            .map {
                when (it.type) {
                    MINUS -> " - "
                    PLUS -> " + "
                    CURRENCY -> " ${it.text}"
                    else -> it.text
                }
            }.joinToString("")
            .replace("( - ", "(-")
            .replace(unaryOperatorAtStart) { "(-${it.groups[1]!!.value})" }
            .replace(unaryOperatorAfterBinary) { "${it.groups[1]!!.value}(-${it.groups[2]!!.value})" }

    private fun List<TokenMatch>.findExpressionType() = when {
        any { it.type == CURRENCY } -> ExpressionType.MULTI_CURRENCY_EXPR
        asSequence()
                .filterNot { it.type == NUMBER }
                .filterNot { it.type == WHITESPACE }
                .any() -> ExpressionType.SINGLE_CURRENCY_EXPR
        else -> ExpressionType.SINGLE_VALUE
    }

    private val zeroNumberRegex = "0+([.,]0+)?".toRegex()

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
                                var exprTokens = tokens.toList()
                                        .dropLast(expressionInput.remainder.count() + fullInput.remainder.count())

                                val involvedCurrencies = exprTokens.asSequence()
                                        .filter { it.type == CURRENCY }
                                        .map { it.text }
                                        .distinct()
                                        .toList()
                                        .ifEmpty { listOf(baseCurrency) }

                                // single value with specified currency normalising (grammar threat this as multicurrency expression)
                                if (exprTokens.count { it.type == CURRENCY } == 1 && exprTokens.lastOrNull()?.type == CURRENCY) {
                                    exprTokens = exprTokens.dropLast(1)
                                }


                                val result = RawInputQuery(
                                        exprTokens.findExpressionType(),
                                        exprTokens.toPrettyPrintExpression(),
                                        expressionResult.setScale(config.currencies.internalPrecision, RoundingMode.HALF_UP),
                                        baseCurrency,
                                        involvedCurrencies,
                                        keys.filterIsInstance<InputKey.Add>().map { it.currencyCode },
                                        keys.filterIsInstance<InputKey.Remove>().map { it.currencyCode }
                                )
                                return Parsed(result, fullInput.remainder)
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