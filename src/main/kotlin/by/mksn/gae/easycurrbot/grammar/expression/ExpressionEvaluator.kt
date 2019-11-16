package by.mksn.gae.easycurrbot.grammar.expression

import by.mksn.gae.easycurrbot.AppConfig
import by.mksn.gae.easycurrbot.exchange.ExchangeRateService
import by.mksn.gae.easycurrbot.util.toConfScale
import java.math.BigDecimal
import java.text.DecimalFormat

enum class ExpressionType {
    SINGLE_VALUE, MULTI_CURRENCY_EXPR, SINGLE_CURRENCY_EXPR
}

data class ExpressionResult(
        val value: BigDecimal,
        val type: ExpressionType,
        val stringRepr: String,
        val baseCurrency: String,
        val involvedCurrencies: List<String>
)


class ExpressionEvaluator(
        private val config: AppConfig,
        private val exchangeRateService: ExchangeRateService
) {

    private val numberFormat = DecimalFormat(config.currencies.outputSumPattern)

    private data class CurrencyMetadata(
            val type: ExpressionType,
            val baseCurrency: String,
            val involvedCurrencies: Set<String>
    )

    private fun captureCurrencyMetadata(rootExpr: Expression): CurrencyMetadata {
        val involvedCurrencies = linkedSetOf<String>()
        val expressionType: ExpressionType?
        val baseCurrency: String?

        fun findCurrencies(expr: Expression): Any = when (expr) {
            is Const -> Unit
            is Negate -> findCurrencies(expr.e)
            is Add -> { findCurrencies(expr.e1); findCurrencies(expr.e2) }
            is Subtract -> { findCurrencies(expr.e1); findCurrencies(expr.e2) }
            is Multiply -> { findCurrencies(expr.e1); findCurrencies(expr.e2) }
            is Divide -> { findCurrencies(expr.e1); findCurrencies(expr.e2) }
            is CurrenciedExpression -> involvedCurrencies.add(expr.currencyCode)
        }

        findCurrencies(rootExpr)
        when (involvedCurrencies.size) {
            0 -> {
                involvedCurrencies.add(config.currencies.apiBase)
                baseCurrency = config.currencies.apiBase
                expressionType = if (rootExpr is Const) ExpressionType.SINGLE_VALUE else ExpressionType.SINGLE_CURRENCY_EXPR
            }
            1 -> {
                baseCurrency = involvedCurrencies.first()
                expressionType = if (rootExpr is CurrenciedExpression && rootExpr.e is Const) {
                    ExpressionType.SINGLE_VALUE
                } else {
                    ExpressionType.SINGLE_CURRENCY_EXPR
                }
            }
            else -> {
                baseCurrency = config.currencies.apiBase
                expressionType = ExpressionType.MULTI_CURRENCY_EXPR
            }
        }
        return CurrencyMetadata(expressionType, baseCurrency, involvedCurrencies)
    }

    private fun createStringRepresentation(rootExpr: Expression, expressionType: ExpressionType): String {

        fun valueFormatWithParsRespect(expr: Expression) =
                if(expr is Negate || expr is Add || expr is Subtract) "(%s)" else "%s"

        fun stringRepr(expr: Expression) : String = when (expr) {
            is Const -> numberFormat.format(expr.number)
            is Negate -> "-${stringRepr(expr.e)}"
            is Add -> (if (expr.e2 is Negate) "%s + (%s)" else "%s + %s")
                    .format(stringRepr(expr.e1), stringRepr(expr.e2))
            is Subtract -> (if (expr.e2 is Negate) "%s - (%s)" else "%s - %s")
                    .format(stringRepr(expr.e1), stringRepr(expr.e2))
            is Multiply -> "${valueFormatWithParsRespect(expr.e1)}*${valueFormatWithParsRespect(expr.e2)}"
                    .format(stringRepr(expr.e1), stringRepr(expr.e2))
            is Divide -> "${valueFormatWithParsRespect(expr.e1)}/${if(expr.e2 is Const) "%s" else "(%s)"}"
                    .format(stringRepr(expr.e1), stringRepr(expr.e2))
            is CurrenciedExpression -> when (expressionType) {
                ExpressionType.SINGLE_VALUE, ExpressionType.SINGLE_CURRENCY_EXPR -> stringRepr(expr.e)
                ExpressionType.MULTI_CURRENCY_EXPR -> if(expr.e is Add || expr.e is Subtract)
                    "(${stringRepr(expr.e)}) ${expr.currencyCode}" else "${stringRepr(expr.e)} ${expr.currencyCode}"
            }
        }

        return stringRepr(rootExpr)
    }

    fun evaluate(rootExpr: Expression): ExpressionResult {
        val (type, base, involved) = captureCurrencyMetadata(rootExpr)

        fun eval(expr: Expression): BigDecimal = when (expr) {
            is Const -> expr.number
            is Negate -> eval(expr.e).negate()
            is Add -> eval(expr.e1) + eval(expr.e2)
            is Subtract -> eval(expr.e1) - eval(expr.e2)
            is Multiply -> eval(expr.e1) * eval(expr.e2)
            is Divide -> eval(expr.e1) / eval(expr.e2)
            is CurrenciedExpression -> if (type == ExpressionType.MULTI_CURRENCY_EXPR) {
                exchangeRateService.exchangeToApiBase(eval(expr.e), expr.currencyCode)
            } else {
                eval(expr.e)
            }
        }

        return ExpressionResult(eval(rootExpr).toConfScale(config), type, createStringRepresentation(rootExpr, type), base, involved.toList())
    }
}