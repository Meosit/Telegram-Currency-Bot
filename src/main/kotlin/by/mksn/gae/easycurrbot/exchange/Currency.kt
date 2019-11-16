package by.mksn.gae.easycurrbot.exchange

import by.mksn.gae.easycurrbot.grammar.expression.ExpressionType
import by.mksn.gae.easycurrbot.input.InputQuery
import java.math.BigDecimal
import java.math.RoundingMode

data class Currency(val code: String, val symbol: String, val aliases: List<String>)

data class ExchangedSum(val currency: Currency, val sum: BigDecimal)

data class ExchangeResults(val input: InputQuery, val rates: List<ExchangedSum>)

fun Currency.toOneUnitInputQuery(internalPrecision: Int, targets: List<String>) = InputQuery(
        rawInput = "1 $code",
        type = ExpressionType.SINGLE_VALUE,
        expression = "1",
        expressionResult = 1.toBigDecimal().setScale(internalPrecision, RoundingMode.HALF_UP),
        baseCurrency = code,
        involvedCurrencies = listOf(code),
        targets = targets
)


