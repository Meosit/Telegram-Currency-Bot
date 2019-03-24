package by.mksn.gae.easycurrbot.service

import by.mksn.gae.easycurrbot.AppConfig
import by.mksn.gae.easycurrbot.entity.InputQuery
import by.mksn.gae.easycurrbot.expr.Expressions
import java.math.RoundingMode
import by.mksn.gae.easycurrbot.entity.Result
import by.mksn.gae.easycurrbot.expr.ExpressionException
import java.lang.ArithmeticException
import java.math.BigDecimal

class InputQueryService(private val config: AppConfig) {

    private val currencyMatchers: Map<String, String> = config.currencies.supported
            .flatMap { c -> c.matchPatterns.map { it.toLowerCase() to c.code } }
            .toMap()

    private val valueTokens = hashSetOf('.', ',', '(', ')', '+', '-', '^', '*', '/', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', ' ', '\t', '\r')
    private val whitespaceRegex = "\\s+".toRegex()
    private val keyStartPostfixRegex = "\\s+[+-]$".toRegex()
    private val unaryOperatorAtStart = "^ - (\\d+)".toRegex()
    private val unaryOperatorAfterBinary = "([-+*^] ?) - (\\d+)".toRegex()
    private val currencyMatcherRegex = "[a-zA-Zа-яА-Я€$]+".toRegex()

    private val expressions = Expressions(config.messages.errors)

    fun parse(query: String): Result<InputQuery, String> {
        val normalizedQuery: String
        val inputExpr: String
        val formattedExpr: String
        val value: BigDecimal

        try {
            normalizedQuery = query.trim()
                    .replace(',', '.')
                    .replace(currencyMatcherRegex) {
                        currencyMatchers[it.value.toLowerCase()] ?: throw IllegalArgumentException(
                                config.messages.errors.invalidMatcherProvided.format(it.value))
                    }

            inputExpr = normalizedQuery
                    .takeWhile { it in valueTokens }
                    .replace(keyStartPostfixRegex, "")

            formattedExpr = inputExpr
                    .replace(whitespaceRegex, "")
                    .replace("+", " + ")
                    .replace("-", " - ")
                    .replace(unaryOperatorAtStart) { "(-${it.groups[1]!!.value})" }
                    .replace(unaryOperatorAfterBinary) { "${it.groups[1]!!.value}(-${it.groups[2]!!.value})" }

            value = expressions.eval(inputExpr).setScale(8, RoundingMode.HALF_EVEN)
        } catch (e: IllegalArgumentException) {
            return Result.error(e.message!!)
        } catch (e: ExpressionException) {
            return Result.error(config.messages.errors.invalidValueProvided.format(e.message))
        } catch (e: ArithmeticException) {
            return Result.error(config.messages.errors.illegalOperationResult)
        }

        val parameters = normalizedQuery.removePrefix(inputExpr).split(whitespaceRegex)
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }

        val base = parameters
                .filterNot { it.startsWith('+') }
                .filterNot { it.startsWith('-') }
                .filterNotNull()
                .firstOrNull()
                ?: config.currencies.base

        val additions = parameters
                .filter { it.startsWith('+') }
                .map { it.removePrefix("+") }
                .filterNotNull()

        val removals = parameters
                .filter { it.startsWith('-') }
                .map { it.removePrefix("-") }
                .filterNotNull()
                .filterNot { it == base }

        val targets = linkedSetOf(base)
        targets.addAll(config.currencies.default)
        targets.addAll(additions)
        targets.removeAll(removals)

        return Result.success(InputQuery(query, formattedExpr, value.abs(), base, targets.toList()))
    }

}
