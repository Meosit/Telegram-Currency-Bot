package by.mksn.gae.easycurrbot.expr.internal

import by.mksn.gae.easycurrbot.AppConfig
import by.mksn.gae.easycurrbot.expr.ExpressionException
import by.mksn.gae.easycurrbot.expr.internal.TokenType.*
import java.math.BigDecimal

internal class Parser(private val tokens: List<Token>, private val messages: AppConfig.Messages.Errors) {

    private var current = 0

    fun parse(): Expr {
        val expr = expression()

        if (!isAtEnd()) {
            throw ExpressionException(messages.expectedEOF, peek().lexeme)
        }

        return expr
    }

    private fun expression(): Expr {
        return addition()
    }

    private fun addition(): Expr {
        var left = multiplication()

        while (match(PLUS, MINUS)) {
            val operator = previous()
            val right = multiplication()

            left = BinaryExpr(left, operator, right)
        }

        return left
    }

    private fun multiplication(): Expr {
        var left = unary()

        while (match(STAR, SLASH)) {
            val operator = previous()
            val right = unary()

            left = BinaryExpr(left, operator, right)
        }

        return left
    }

    private fun unary(): Expr {
        if (match(MINUS)) {
            val operator = previous()
            val right = unary()

            return UnaryExpr(operator, right)
        }

        return exponent()
    }

    private fun exponent(): Expr {
        var left = primary()

        if (match(EXPONENT)) {
            val operator = previous()
            val right = unary()

            left = BinaryExpr(left, operator, right)
        }

        return left
    }

    private fun primary(): Expr {
        if (match(NUMBER)) {
            return LiteralExpr(previous().literal as BigDecimal)
        }

        if (match(LEFT_PAREN)) {
            val expr = expression()

            if (check(RIGHT_PAREN)) {
                advance()
            } else {
                throw ExpressionException(messages.unclosedParentheses, previous().lexeme)
            }

            return GroupingExpr(expr)
        }

        if (current == 0) {
            throw ExpressionException(messages.emptyLeftOperand)
        }

        throw ExpressionException(messages.invalidRightOperand, previous().lexeme)
    }

    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()

                return true
            }
        }

        return false
    }

    private fun check(tokenType: TokenType): Boolean {
        return if (isAtEnd()) {
            false
        } else {
            peek().type === tokenType
        }
    }

    private fun advance(): Token {
        if (!isAtEnd()) current++

        return previous()
    }

    private fun isAtEnd() = peek().type == EOF

    private fun peek() = tokens[current]

    private fun previous() = tokens[current - 1]

}