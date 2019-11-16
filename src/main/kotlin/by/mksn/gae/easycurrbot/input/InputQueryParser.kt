package by.mksn.gae.easycurrbot.input

import by.mksn.gae.easycurrbot.AppConfig
import by.mksn.gae.easycurrbot.exchange.ExchangeRateService
import by.mksn.gae.easycurrbot.grammar.BotInputGrammar
import by.mksn.gae.easycurrbot.grammar.expression.ExpressionEvaluator
import by.mksn.gae.easycurrbot.util.Result
import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.Parsed

class InputQueryParser(
        private val config: AppConfig,
        exchangeRateService: ExchangeRateService
) {

    private val grammar = BotInputGrammar(config)

    private val expressionEvaluator = ExpressionEvaluator(config, exchangeRateService)

    private fun Set<String>.toPredefinedOrderList() =
            config.currencies.supported.asSequence().map { it.code }.filter { this.contains(it) }.toList()

    fun parse(query: String): Result<InputQuery, InputError> {
        val rawInput = query.trim().replace("\n", " ")
        return when (val parseResult = grammar.tryParseToEnd(rawInput)) {
            is Parsed -> with(parseResult.value) {
                try {
                    val (value, type, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expression)
                    val targets = involvedCurrencies.toMutableSet()

                    targets.addAll(config.currencies.default)
                    targets.addAll(additionalCurrencies)

                    Result.success(InputQuery(
                            rawInput = rawInput,
                            type = type,
                            expression = stringRepr,
                            expressionResult = value,
                            baseCurrency = baseCurrency,
                            involvedCurrencies = involvedCurrencies,
                            targets = targets.toPredefinedOrderList()
                    ))
                } catch (_: ArithmeticException) {
                    Result.failure(InputError(rawInput, 1, config.strings.errors.divisionByZero))
                }
            }
            is ErrorResult -> Result.failure(parseResult.toInputError(rawInput, config))
        }
    }

}
