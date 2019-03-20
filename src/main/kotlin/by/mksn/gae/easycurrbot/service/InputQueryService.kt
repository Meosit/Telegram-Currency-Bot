package by.mksn.gae.easycurrbot.service

import by.mksn.gae.easycurrbot.AppConfig
import by.mksn.gae.easycurrbot.entity.InputQuery
import by.mksn.gae.easycurrbot.expr.Expressions
import java.math.RoundingMode
import by.mksn.gae.easycurrbot.entity.Result
import by.mksn.gae.easycurrbot.expr.ExpressionException
import java.lang.ArithmeticException

class InputQueryService(private val config: AppConfig) {

    private val availableCurrencies: Map<String, String> = config.currencies.supported
            .flatMap { c -> c.matchPatterns.map { it.toLowerCase() to c.code } }
            .toMap()

    private val valueTokens = hashSetOf('.', ',', '(',')', '+', '-', '^', '*', '/', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    private val whitespaceRegex = "\\s+".toRegex()

    private val expressions = Expressions(config.messages.expressions)

    fun parse(query: String): Result<InputQuery, String> {
        val expr = query.trim().takeWhile { it in valueTokens }.replace(',', '.')
        val value = try {
            expressions.eval(expr).setScale(4, RoundingMode.HALF_DOWN)
        } catch (e: ExpressionException) {
            return Result.error(String.format(config.messages.expressions.invalidValueProvided, e.message))
        } catch (e: ArithmeticException) {
            return Result.error(config.messages.expressions.illegalOperationResult)
        }
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
                ?: config.currencies.base

        val additions = parameters
                .filter { it.startsWith('+') }
                .map { it.removePrefix("+") }
                .map { it.toLowerCase() }
                .map { availableCurrencies[it] }
                .filterNotNull()

        val removals = parameters
                .filter { it.startsWith('-') }
                .map { it.removePrefix("-") }
                .map { it.toLowerCase() }
                .map { availableCurrencies[it] }
                .filterNotNull()
                .filterNot { it == base }

        val targets = linkedSetOf(base)
        targets.addAll(config.currencies.default)
        targets.addAll(additions)
        targets.removeAll(removals)
        return Result.success(InputQuery(query, expr, value.abs(), base, targets.toList()))
    }

}
