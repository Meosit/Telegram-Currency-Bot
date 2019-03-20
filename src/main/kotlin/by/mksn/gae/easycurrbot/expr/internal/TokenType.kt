package by.mksn.gae.easycurrbot.expr.internal

internal enum class TokenType {

    // Basic operators
    PLUS,
    MINUS,
    STAR,
    SLASH,
    EXPONENT,

    // Parentheses
    LEFT_PAREN,
    RIGHT_PAREN,

    // Literals
    NUMBER,

    EOF

}