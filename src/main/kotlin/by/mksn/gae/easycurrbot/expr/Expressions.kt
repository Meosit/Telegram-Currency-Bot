package by.mksn.gae.easycurrbot.expr

import by.mksn.gae.easycurrbot.AppConfig
import by.mksn.gae.easycurrbot.expr.internal.*
import java.math.BigDecimal
import java.math.MathContext

class ExpressionException(message: String, vararg args: Any)
    : RuntimeException(String.format(message, *args))

class Expressions(private val messages: AppConfig.Messages.Errors) {

    private val mathContext = MathContext.DECIMAL64

    fun eval(expression: String): BigDecimal {
        if (expression.isBlank()) throw ExpressionException(messages.emptyExpression)

        val evaluator = Evaluator(mathContext, messages)

        return evaluator.eval(parse(expression))
    }

    private fun parse(expression: String): Expr {
        return parse(scan(expression))
    }

    private fun parse(tokens: List<Token>): Expr {
        return Parser(tokens, messages).parse()
    }

    private fun scan(expression: String): List<Token> {
        return Scanner(expression, mathContext, messages).scanTokens()
    }

}