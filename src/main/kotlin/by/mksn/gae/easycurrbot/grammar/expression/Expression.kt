package by.mksn.gae.easycurrbot.grammar.expression
import java.math.BigDecimal


sealed class Expression {
    abstract val stringRepr: String

    internal fun stringReprWithPars() = when (this) {
        is Const, is CurrenciedExpression -> stringRepr
        else -> "(${stringRepr})"
    }
}

data class CurrenciedExpression(val e: Expression, val currencyCode: String) : Expression() {
    override val stringRepr = "${e.stringReprWithPars()} $currencyCode"
}

data class Const(
        val number: BigDecimal,
        override val stringRepr: String = number.toString()
) : Expression()

data class Add(val e1: Expression, val e2: Expression) : Expression() {
    override val stringRepr = "${e1.stringReprWithPars()} + ${e2.stringReprWithPars()}"
}

data class Subtract(val e1: Expression, val e2: Expression) : Expression() {
    override val stringRepr = "${e1.stringReprWithPars()} - ${e2.stringReprWithPars()}"
}

data class Multiply(val e1: Expression, val e2: Expression) : Expression() {
    override val stringRepr = "${e1.stringReprWithPars()}*${e2.stringReprWithPars()}"
}

data class Divide(val e1: Expression, val e2: Expression) : Expression() {
    override val stringRepr = "${e1.stringReprWithPars()}/${e2.stringReprWithPars()}"
}

data class Negate(val e: Expression) : Expression() {
    override val stringRepr = "-${e.stringReprWithPars()}"
}