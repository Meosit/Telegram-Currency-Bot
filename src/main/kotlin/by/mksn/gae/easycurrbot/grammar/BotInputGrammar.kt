package by.mksn.gae.easycurrbot.grammar


import by.mksn.gae.easycurrbot.AppConfig
import by.mksn.gae.easycurrbot.grammar.expression.Const
import by.mksn.gae.easycurrbot.grammar.expression.CurrenciedExpression
import by.mksn.gae.easycurrbot.grammar.expression.Expression
import by.mksn.gae.easycurrbot.grammar.parsers.CurrenciedMathParsers
import by.mksn.gae.easycurrbot.grammar.parsers.InvalidCurrencyFoundException
import by.mksn.gae.easycurrbot.grammar.parsers.SimpleMathParsers
import by.mksn.gae.easycurrbot.grammar.parsers.TokenDictionary
import by.mksn.gae.easycurrbot.input.CurrencyAliasMatcher
import by.mksn.gae.easycurrbot.util.toConfScaledBigDecimal
import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.lexer.TokenMatch
import com.github.h0tk3y.betterParse.parser.Parser

data class BotInput(
        val expression: Expression,
        val additionalCurrencies: List<String>
)

@Suppress("PrivatePropertyName")
class BotInputGrammar(private val config: AppConfig) : Grammar<BotInput>() {

    private val currencyAliasMatcher = CurrencyAliasMatcher(config)

    private val tokenDict = TokenDictionary(config, currencyAliasMatcher.allAliasesRegex)
    private val mathParsers = SimpleMathParsers(config, tokenDict)
    private val currParsers = CurrenciedMathParsers(tokenDict, mathParsers, currencyAliasMatcher)

    private val keyPrefix = skip(tokenDict.plus or tokenDict.exclamation or tokenDict.ampersand or tokenDict.nativeConversionUnion)
    private val currencyKey = skip(tokenDict.whitespace) and (keyPrefix and currParsers.currency) map { it }
    private val additionaCurrenciesChain by zeroOrMore(currencyKey)

    private val onlyCurrencyExpressionParser by currParsers.currency map {
        CurrenciedExpression(Const(1.toConfScaledBigDecimal(config)), it)
    }

    private val singleCurrencyExpressionParser by mathParsers.subSumChain and optional(currParsers.currency) map { (expr, currency) ->
        CurrenciedExpression(expr, currency ?: config.currencies.apiBase)
    }

    private val multiCurrencyExpressionParser by currParsers.currenciedSubSumChain
    private val allValidExpressionParsers by multiCurrencyExpressionParser or singleCurrencyExpressionParser or onlyCurrencyExpressionParser
    private val botInputParser by allValidExpressionParsers and additionaCurrenciesChain map { (expr, keys) -> BotInput(expr, keys) }

    override val tokens = tokenDict.allTokens
    override val rootParser = object : Parser<BotInput> {
        override fun tryParse(tokens: Sequence<TokenMatch>) =
                try {
                    botInputParser.tryParse(tokens)
                } catch (e: InvalidCurrencyFoundException) {
                    e.toErrorResult()
                }
    }

}