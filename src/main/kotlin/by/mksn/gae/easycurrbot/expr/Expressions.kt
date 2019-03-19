package by.mksn.gae.easycurrbot.expr

import by.mksn.gae.easycurrbot.expr.internal.*
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

class ExpressionException(message: String)
    : RuntimeException(message)

@Suppress("unused")
class Expressions {

    private var mathContext = MathContext.DECIMAL64
    val precision: Int
        get() = mathContext.precision

    val roundingMode: RoundingMode
        get() = mathContext.roundingMode

    fun setPrecision(precision: Int): Expressions {
        mathContext = MathContext(precision, mathContext.roundingMode)

        return this
    }

    fun setRoundingMode(roundingMode: RoundingMode): Expressions {
        mathContext = MathContext(mathContext.precision, roundingMode)

        return this
    }

    fun eval(expression: String): BigDecimal {
        if (expression.isBlank()) throw ExpressionException("Expression expected")

        val evaluator = Evaluator(mathContext)

        return evaluator.eval(parse(expression))
    }

    private fun parse(expression: String): Expr {
        return parse(scan(expression))
    }

    private fun parse(tokens: List<Token>): Expr {
        return Parser(tokens).parse()
    }

    private fun scan(expression: String): List<Token> {
        return Scanner(expression, mathContext).scanTokens()
    }

}