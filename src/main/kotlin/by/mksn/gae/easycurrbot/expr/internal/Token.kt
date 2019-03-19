package by.mksn.gae.easycurrbot.expr.internal

internal class Token(val type: TokenType,
                     val lexeme: String,
                     val literal: Any?) {

    override fun toString(): String {
        return "$type $lexeme $literal"
    }

}