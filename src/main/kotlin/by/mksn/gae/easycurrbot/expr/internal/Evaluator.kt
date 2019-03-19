package by.mksn.gae.easycurrbot.expr.internal

import by.mksn.gae.easycurrbot.expr.ExpressionException
import by.mksn.gae.easycurrbot.expr.internal.TokenType.*
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

internal class Evaluator(private val mathContext: MathContext) : ExprVisitor<BigDecimal> {

    fun eval(expr: Expr): BigDecimal {
        return expr.accept(this)
    }


    override fun visitBinaryExpr(expr: BinaryExpr): BigDecimal {
        val left = eval(expr.left)
        val right = eval(expr.right)

        return when (expr.operator.type) {
            PLUS -> left + right
            MINUS -> left - right
            STAR -> left * right
            SLASH -> left.divide(right, mathContext)
            MODULO -> left.remainder(right, mathContext)
            EXPONENT -> left pow right
            else -> throw ExpressionException(
                    "Invalid binary operator '${expr.operator.lexeme}'")
        }
    }

    override fun visitUnaryExpr(expr: UnaryExpr): BigDecimal {
        val right = eval(expr.right)

        return when (expr.operator.type) {
            MINUS -> {
                right.negate()
            }
            else -> throw ExpressionException("Invalid unary operator")
        }
    }

    override fun visitLiteralExpr(expr: LiteralExpr): BigDecimal {
        return expr.value
    }

    override fun visitGroupingExpr(expr: GroupingExpr): BigDecimal {
        return eval(expr.expression)
    }

    private infix fun Expr.or(right: Expr): BigDecimal {
        val left = eval(this)

        // short-circuit if left is truthy
        if (left.isTruthy()) return BigDecimal.ONE

        return eval(right).isTruthy().toBigDecimal()
    }

    private infix fun Expr.and(right: Expr): BigDecimal {
        val left = eval(this)

        // short-circuit if left is falsey
        if (!left.isTruthy()) return BigDecimal.ZERO

        return eval(right).isTruthy().toBigDecimal()
    }

    private fun BigDecimal.isTruthy(): Boolean {
        return this != BigDecimal.ZERO
    }

    private fun Boolean.toBigDecimal(): BigDecimal {
        return if (this) BigDecimal.ONE else BigDecimal.ZERO
    }

    private infix fun BigDecimal.pow(n: BigDecimal): BigDecimal {
        var right = n
        val signOfRight = right.signum()
        right = right.multiply(signOfRight.toBigDecimal())
        val remainderOfRight = right.remainder(BigDecimal.ONE)
        val n2IntPart = right.subtract(remainderOfRight)
        val intPow = pow(n2IntPart.intValueExact(), mathContext)
        val doublePow = BigDecimal(Math
                .pow(toDouble(), remainderOfRight.toDouble()))

        var result = intPow.multiply(doublePow, mathContext)
        if (signOfRight == -1) result = BigDecimal
                .ONE.divide(result, mathContext.precision, RoundingMode.HALF_UP)

        return result
    }

}