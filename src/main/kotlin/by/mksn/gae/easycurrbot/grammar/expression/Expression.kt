package by.mksn.gae.easycurrbot.grammar.expression
import java.math.BigDecimal


sealed class Expression

data class CurrenciedExpression(val e: Expression, val currencyCode: String) : Expression()

data class Const(val number: BigDecimal) : Expression()

data class Add(val e1: Expression, val e2: Expression) : Expression()

data class Subtract(val e1: Expression, val e2: Expression) : Expression()

data class Multiply(val e1: Expression, val e2: Expression) : Expression()

data class Divide(val e1: Expression, val e2: Expression) : Expression()

data class Negate(val e: Expression) : Expression()