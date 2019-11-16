package by.mksn.gae.easycurrbot.input

import by.mksn.gae.easycurrbot.grammar.expression.ExpressionType
import java.math.BigDecimal

data class InputQuery(
        val rawInput: String,
        val type: ExpressionType,
        val expression: String,
        val expressionResult: BigDecimal,
        val baseCurrency: String,
        val involvedCurrencies: List<String>,
        val targets: List<String>
)

fun InputQuery.isOneUnit() = type == ExpressionType.SINGLE_VALUE && expression == "1"