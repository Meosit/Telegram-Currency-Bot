package by.mksn.gae.easycurrbot.expr.internal

import by.mksn.gae.easycurrbot.AppConfig
import java.math.MathContext
import by.mksn.gae.easycurrbot.expr.ExpressionException
import by.mksn.gae.easycurrbot.expr.internal.TokenType.*

internal class Scanner(private val source: String,
                       private val mathContext: MathContext,
                       private val messages: AppConfig.Messages.Errors) {

    private val tokens: MutableList<Token> = mutableListOf()
    private var start = 0
    private var current = 0

    fun scanTokens(): List<Token> {
        while (!isAtEnd()) {
            scanToken()
        }

        tokens.add(Token(EOF, "", null))
        return tokens
    }

    private fun isAtEnd(): Boolean {
        return current >= source.length
    }

    private fun scanToken() {
        start = current
        val c = advance()

        when (c) {
            ' ', '\r', '\t' -> { /* Ignore whitespace. */ }
            '+' -> addToken(PLUS)
            '-' -> addToken(MINUS)
            '*' -> addToken(STAR)
            '/' -> addToken(SLASH)
            '^' -> addToken(EXPONENT)
            '(' -> addToken(LEFT_PAREN)
            ')' -> addToken(RIGHT_PAREN)
            else -> {
                when {
                    c.isDigit() -> number()
                    else -> throw ExpressionException(messages.invalidToken, c)
                }
            }
        }
    }

    private fun number() {
        while (peek().isDigit()) advance()

        if (peek() == '.' && peekNext().isDigit()) {
            advance()

            while (peek().isDigit()) advance()
        }

        val value = source
                .substring(start, current)
                .toBigDecimal(mathContext)

        addToken(NUMBER, value)
    }

    private fun advance() = source[current++]

    private fun peek(): Char {
        return if (isAtEnd()) {
            '\u0000'
        } else {
            source[current]
        }
    }

    private fun peekNext(): Char {
        return if (current + 1 >= source.length) {
            '\u0000'
        } else {
            source[current + 1]
        }
    }

    private fun addToken(type: TokenType) = addToken(type, null)

    private fun addToken(type: TokenType, literal: Any?) {
        val text = source.substring(start, current)
        tokens.add(Token(type, text, literal))
    }

}