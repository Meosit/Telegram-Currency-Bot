package by.mksn.gae.easycurrbot.service

import by.mksn.gae.easycurrbot.AppConfig
import by.mksn.gae.easycurrbot.entity.InputError
import by.mksn.gae.easycurrbot.entity.InputQuery
import by.mksn.gae.easycurrbot.entity.Result
import by.mksn.gae.easycurrbot.entity.trimToLength
import by.mksn.gae.easycurrbot.grammar.DivisionByZero
import by.mksn.gae.easycurrbot.grammar.InputExpressionGrammar
import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.*

class InputQueryService(
        private val config: AppConfig,
        exchangeRateService: ExchangeRateService
) {

    private val currencyAliases: Map<String, String> = config.currencies.supported
            .flatMap { c -> c.aliases.map { it.toLowerCase() to c.code } }
            .toMap()

    private val currencyAliasRegex = "[a-zA-Zа-яА-Я€$]+".toRegex()
    private val grammar = InputExpressionGrammar(config, exchangeRateService)


    private fun normalizeQuery(query: String): Result<Pair<String, Int>, InputError> {
        var result = query.replace(',', '.')
        var errorPositionCorrection = 0
        var lastPositionCorrection = 0
        for(match in currencyAliasRegex.findAll(result).distinct()) {
            val currency = currencyAliases[match.value.toLowerCase()]
            if (currency == null) {
                return Result.failure(InputError(
                        rawInput = query.trimToLength(config.telegram.outputWidthChars, tail = "…"),
                        errorPosition = match.range.start + 1,
                        message = config.strings.errors.invalidMatcherProvided.format(match.value)
                ))
            } else {
                result = result.replace(match.value, currency)
            }
            lastPositionCorrection = match.value.length - currency.length
            errorPositionCorrection += lastPositionCorrection
        }
        return Result.success(result to (errorPositionCorrection - lastPositionCorrection))
    }

    private fun ErrorResult.toInputError(rawInput: String, errorPositionCorrection: Int): InputError = when {
        this is UnparsedRemainder -> InputError(
                rawInput = rawInput.trimToLength(config.telegram.outputWidthChars, tail = "…"),
                errorPosition = startsWith.column + errorPositionCorrection,
                message = if (grammar.isCurrency(startsWith)) config.strings.errors.illegalCurrencyPlacement else config.strings.errors.unparsedReminder
        )
        this is MismatchedToken -> InputError(
                rawInput = rawInput.trimToLength(config.telegram.outputWidthChars, tail = "…"),
                errorPosition = found.column + errorPositionCorrection,
                message = config.strings.errors.mismatchedToken.format(found.text, expected.name)
        )
        this is NoMatchingToken -> InputError(
                rawInput = rawInput.trimToLength(config.telegram.outputWidthChars, tail = "…"),
                errorPosition = tokenMismatch.column + errorPositionCorrection,
                message = config.strings.errors.noMatchingToken.format(tokenMismatch.text)
        )
        this is UnexpectedEof -> InputError(
                rawInput = rawInput.trimToLength(config.telegram.outputWidthChars, tail = "…"),
                errorPosition = rawInput.trim().length + errorPositionCorrection,
                message = config.strings.errors.unexpectedEOF.format(expected.name)
        )
        this is DivisionByZero -> InputError(
                rawInput = rawInput.trimToLength(config.telegram.outputWidthChars, tail = "…"),
                errorPosition = zeroToken.column + errorPositionCorrection,
                message = config.strings.errors.divisionByZero
        )
        this is AlternativesFailure -> {
            fun find(errors: List<ErrorResult>): ErrorResult {
                val error = errors.last()
                return if (error !is AlternativesFailure) error else find(error.errors)
            }
            find(errors).toInputError(rawInput, errorPositionCorrection)
        }
        else -> InputError(
                rawInput = rawInput.trimToLength(config.telegram.outputWidthChars, tail = "…"),
                errorPosition = rawInput.trim().length,
                message = config.strings.errors.unexpectedError
        )
    }

    fun parse(query: String): Result<InputQuery, InputError> {
        val rawInput = query.trim().replace("\n", " ")
        val normalizedQueryResult = normalizeQuery(rawInput)
        val (normalizedQuery, errorPositionCorrection) = when (normalizedQueryResult) {
            is Result.Success -> normalizedQueryResult.value
            is Result.Failure -> return Result.failure(normalizedQueryResult.error)
        }
        val parseResult = grammar.tryParseToEnd(normalizedQuery)
        return when (parseResult) {
            is Parsed -> with(parseResult.value) {
                val targets = involvedCurrencies.toMutableSet()
                targets.addAll(config.currencies.default)
                targets.addAll(addCurrencies)
                targets.removeAll(removeCurrencies)

                Result.success(InputQuery(
                        rawInput = rawInput,
                        type = type,
                        expression = expression,
                        expressionResult = expressionResult,
                        baseCurrency = baseCurrency,
                        involvedCurrencies = involvedCurrencies,
                        targets = targets.toList()
                ))
            }
            is ErrorResult -> Result.failure(parseResult.toInputError(rawInput, errorPositionCorrection))
        }
    }

}
