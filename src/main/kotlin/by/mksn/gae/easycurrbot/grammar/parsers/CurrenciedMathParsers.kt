package by.mksn.gae.easycurrbot.grammar.parsers

import by.mksn.gae.easycurrbot.grammar.expression.*
import by.mksn.gae.easycurrbot.input.CurrencyAliasMatcher
import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser

/**
 * Container class for the parsers of the basic currencied math expression terms
 */
class CurrenciedMathParsers(
        tokenDict: TokenDictionary,
        mathParsers: SimpleMathParsers,
        currencyAliasMatcher: CurrencyAliasMatcher
) {

    val currency: Parser<String> = tokenDict.currency or tokenDict.invalidCurrencyToken map {
        if (it.type == tokenDict.invalidCurrencyToken) {
            throw InvalidCurrencyFoundException(it)
        }
        currencyAliasMatcher.matchToCode(it.text)
    }

    private val currenciedTerm: Parser<Expression> =
            (currency and mathParsers.divMulChain  map { (curr, expr) -> CurrenciedExpression(expr, curr) }) or
            (mathParsers.divMulChain and currency map { (expr, curr) -> CurrenciedExpression(expr, curr) }) or
            (skip(tokenDict.minus) and parser(this::currenciedTerm) map { Negate(it) }) or
            (skip(tokenDict.leftPar) and parser(this::currenciedSubSumChain) and skip(tokenDict.rightPar))

    // division and multiplication can be performed only with simple numbers
    private val currenciedDivMulChain: Parser<Expression> = (currenciedTerm and oneOrMore((tokenDict.divide or tokenDict.multiply) and mathParsers.term) map { (initial, operands) ->
        operands.fold(initial) { a, (op, b) -> if(op.type == tokenDict.multiply) Multiply(a, b) else Divide(a, b) }
    }) or currenciedTerm

    val currenciedSubSumChain: Parser<Expression> = leftAssociative(currenciedDivMulChain, tokenDict.plus or tokenDict.minus use { type }) { a, op, b -> if (op == tokenDict.plus) Add(a, b) else Subtract(a, b) }
}