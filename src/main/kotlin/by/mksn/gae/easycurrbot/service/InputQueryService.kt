package by.mksn.gae.easycurrbot.service

import by.mksn.gae.easycurrbot.AppConfig
import by.mksn.gae.easycurrbot.entity.InputError
import by.mksn.gae.easycurrbot.entity.InputQuery
import by.mksn.gae.easycurrbot.entity.Result
import by.mksn.gae.easycurrbot.extensions.trimToLength
import by.mksn.gae.easycurrbot.grammar.DivisionByZero
import by.mksn.gae.easycurrbot.grammar.InputExpressionGrammar
import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.*

class InputQueryService(
        private val config: AppConfig,
        exchangeRateService: ExchangeRateService
) {

    private val currencyAliasMatcher = CurrencyAliasMatcher(config)

    private val spaceInNumberRegex = "[0-9,.](\\s+)[0-9.,]".toRegex()
    private val kiloSuffixes = charArrayOf('k', 'K', 'к', 'К')
    private val grammar = InputExpressionGrammar(config, exchangeRateService)

    private fun removeWhitespacesFromNumbers(query: String): String {
        var result = query
        var nextSearchStart = 0
        var match = spaceInNumberRegex.find(result, nextSearchStart)
        while (match != null) {
            val spaceMatch = match.groups[1]!!
            result = result.removeRange(spaceMatch.range)
            nextSearchStart = match.range.endInclusive - spaceMatch.value.length + 1
            match = spaceInNumberRegex.find(result, nextSearchStart)
        }
        return result
    }

    private fun normalizeQuery(query: String): Result<Pair<String, Int>, InputError> {
        var result = query
        var lastPositionCorrection = 0
        var nextSearchStart = 0
        var match = CurrencyAliasMatcher.CURRENCY_ALIAS_REGEX.find(result, nextSearchStart)
        var currencyAtQueryEnd = false
        while (match != null) {
            currencyAtQueryEnd = (match.range.endInclusive == result.lastIndex)
            if (match.value.all { it in kiloSuffixes }) {
                result = result.replaceRange(match.range, config.strings.kiloSpecialChar.repeat(match.value.length))
                lastPositionCorrection = 0
            } else {
                var kiloSuffixCount = 0
                var normalizedMatch = match.value.toLowerCase()
                var currency = currencyAliasMatcher.matchToCurrencyCode(match.value)
                while (currency == null && normalizedMatch.length > 1 && normalizedMatch[0] in kiloSuffixes) {
                    normalizedMatch = normalizedMatch.drop(1)
                    currency = currencyAliasMatcher.matchToCurrencyCode(normalizedMatch)
                    kiloSuffixCount++
                }
                if (currency == null) {
                    return Result.failure(InputError(
                            rawInput = query.trimToLength(config.telegram.outputWidthChars, tail = "…"),
                            errorPosition = match.range.start + 1 + lastPositionCorrection,
                            message = config.strings.errors.invalidMatcherProvided.format(match.value)
                    ))
                } else {
                    result = result.replaceRange(match.range, config.strings.kiloSpecialChar.repeat(kiloSuffixCount) + currency)
                }

                lastPositionCorrection = match.value.length - (currency.length + kiloSuffixCount)
            }

            nextSearchStart = match.range.endInclusive - lastPositionCorrection + 1
            match = CurrencyAliasMatcher.CURRENCY_ALIAS_REGEX.find(result, nextSearchStart)
        }
        val errorPositionCorrection = query.length - result.length - if (currencyAtQueryEnd) lastPositionCorrection else 0
        return Result.success(result to errorPositionCorrection)
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

    private fun Set<String>.toPredefinedOrderList() =
            config.currencies.supported.asSequence().map { it.code }.filter { this.contains(it) }.toList()

    fun parse(query: String): Result<InputQuery, InputError> {
        val rawInput = query.trim().replace("\n", " ")
        val normalizedQueryResult = normalizeQuery(rawInput)
        val (normalizedQuery, errorPositionCorrection) = when (normalizedQueryResult) {
            is Result.Success -> normalizedQueryResult.value
            is Result.Failure -> return Result.failure(normalizedQueryResult.error)
        }
        return when (val parseResult = grammar.tryParseToEnd(normalizedQuery)) {
            is Parsed -> with(parseResult.value) {
                val targets = involvedCurrencies.toMutableSet()
                targets.addAll(config.currencies.default)
                targets.addAll(addCurrencies)

                Result.success(InputQuery(
                        rawInput = rawInput,
                        type = type,
                        expression = expression,
                        expressionResult = expressionResult,
                        baseCurrency = baseCurrency,
                        involvedCurrencies = involvedCurrencies,
                        targets = targets.toPredefinedOrderList()
                ))
            }
            is ErrorResult -> Result.failure(parseResult.toInputError(rawInput, errorPositionCorrection))
        }
    }

}
