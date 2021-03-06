package by.mksn.gae.easycurrbot.grammar.parsers

import com.github.h0tk3y.betterParse.lexer.TokenMatch
import com.github.h0tk3y.betterParse.parser.NoMatchingToken
import com.github.h0tk3y.betterParse.parser.UnparsedRemainder

/**
 * Represents the situation when the invalid alias placed instead of a currency.
 * Such logic required since the grammar in the most cases returns [UnparsedRemainder] instead of [NoMatchingToken]
 */
class InvalidCurrencyFoundException(private val aliasToken: TokenMatch) : RuntimeException() {
    fun toErrorResult() = NoMatchingToken(aliasToken)
}