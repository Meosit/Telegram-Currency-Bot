package by.mksn.gae.easycurrbot.service

import by.mksn.gae.easycurrbot.config.CurrenciesConfig
import by.mksn.gae.easycurrbot.expr.ExpressionException
import by.mksn.gae.easycurrbot.expr.Expressions
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * @author Mikhail Snitavets
 */
class InputQueryService(currConf: CurrenciesConfig) {

    private val defaultBaseCurrency = currConf.currencies.base
    private val defaultCurrencies: List<String> = currConf.currencies.default
    private val availableCurrencies: Map<String, String> = currConf.currencies.supported
            .flatMap { c -> c.matchPatterns.map { it.toLowerCase() to c.code } }
            .toMap()

    private val valueTokens = hashSetOf('.', '(',')', '+', '-', '^', '*', '/', '%', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    private val whitespaceRegex = "\\s+".toRegex()

    private val expressions = Expressions()

    fun parse(query: String): InputQuery? {
        val expr = query.trim().takeWhile { it in valueTokens }
        val value = try {
            expressions.eval(expr).setScale(4, RoundingMode.HALF_DOWN)
        } catch (e: ExpressionException) {
            null
        } catch (e: ArrayIndexOutOfBoundsException) {
            null
        }

        if (value != null) {
            val parameters = query.removePrefix(expr)
                    .split(whitespaceRegex)
                    .asSequence()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

            val base = parameters
                    .filterNot { it.startsWith('+') }
                    .filterNot { it.startsWith('-') }
                    .map { it.toLowerCase() }
                    .map { availableCurrencies[it] }
                    .filterNotNull()
                    .firstOrNull()
                    ?: defaultBaseCurrency

            val additions = parameters
                    .filter { it.startsWith('+') }
                    .map { it.removePrefix("+") }
                    .map { it.toLowerCase() }
                    .map { availableCurrencies[it] }
                    .filterNotNull()
            val removals = parameters
                    .filter { it.startsWith('-') }
                    .map { it.removePrefix("-") }
                    .filterNot { it == base }
                    .map { it.toLowerCase() }
                    .map { availableCurrencies[it] }
                    .filterNotNull()
            val targets = linkedSetOf(base)
            targets.addAll(defaultCurrencies)
            targets.addAll(additions)
            targets.removeAll(removals)
            if (targets.isEmpty()) {
                return null
            }
            return InputQuery(value.abs(), base, targets.toList())
        } else {
            return null
        }
    }

}

data class InputQuery(val value: BigDecimal, val base: String, val targets: List<String>)
