package by.mksn.gae.easycurrbot.entity

import java.math.BigDecimal
import java.math.RoundingMode


enum class ExpressionType {
    SINGLE_VALUE, MULTI_CURRENCY_EXPR, SINGLE_CURRENCY_EXPR
}

data class RawInputQuery(
        val type: ExpressionType,
        val expression: String,
        val expressionResult: BigDecimal,
        val baseCurrency: String,
        val involvedCurrencies: List<String>,
        val addCurrencies: List<String>,
        val removeCurrencies: List<String>
)

data class InputQuery(
        val rawInput: String,
        val type: ExpressionType,
        val expression: String,
        val expressionResult: BigDecimal,
        val baseCurrency: String,
        val involvedCurrencies: List<String>,
        val targets: List<String>
) {
    fun isOneUnit() = type == ExpressionType.SINGLE_VALUE && expression == "1"
}

data class InputError(
        val rawInput: String,
        val errorPosition: Int,
        val message: String
) {
    fun toMarkdown() = """
        ${message.escapeMarkdown()} (at $errorPosition)
        ```  ${"▼".padStart(if (errorPosition > rawInput.length) rawInput.length else errorPosition)}
        > $rawInput
          ${"▲".padStart(if (errorPosition > rawInput.length) rawInput.length else errorPosition)}```
    """.trimIndent()

    fun toSingleLine() = "(at $errorPosition) $rawInput"

    private fun String.escapeMarkdown() = replace("*", "\\*")
            .replace("_", "\\_")
            .replace("`", "\\`")
}

data class ExchangedSum(val currency: Currency, val sum: BigDecimal)

data class ExchangeResults(val input: InputQuery, val rates: List<ExchangedSum>)

data class Currency(val code: String, val symbol: String, val aliases: List<String>)

fun Currency.toOneUnitInputQuery(internalPrecision: Int, targets: List<String>) = InputQuery(
        rawInput = "1 $code",
        type = ExpressionType.SINGLE_VALUE,
        expression = "1",
        expressionResult = 1.toBigDecimal().setScale(internalPrecision, RoundingMode.HALF_UP),
        baseCurrency = code,
        involvedCurrencies = listOf(code),
        targets = targets
)


