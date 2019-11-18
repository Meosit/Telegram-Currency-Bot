package by.mksn.gae.easycurrbot.grammar.parsers

import by.mksn.gae.easycurrbot.AppConfig
import by.mksn.gae.easycurrbot.grammar.expression.*
import by.mksn.gae.easycurrbot.util.toConfScaledBigDecimal
import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser

/**
 * Container class for the parsers of the basic math expression terms
 */
class SimpleMathParsers(config: AppConfig, tokenDict: TokenDictionary) {

    private fun String.toParsableNumber() = replace(" ", "").replace(',', '.')

    val number: Parser<Expression> = tokenDict.number and zeroOrMore(tokenDict.kilo or tokenDict.mega) map { (num, suffixes) ->
        val number = suffixes.foldRight(num.text.toParsableNumber().toConfScaledBigDecimal(config)) { suffix, acc ->
            acc * (if (suffix.type == tokenDict.kilo) 1_000 else 1_000_000).toConfScaledBigDecimal(config)
        }
        Const(number)
    }

    val term: Parser<Expression> = number or
            (skip(tokenDict.minus) and parser(this::term) map { Negate(it) }) or
            (skip(tokenDict.leftPar) and parser(this::subSumChain) and skip(tokenDict.rightPar))

    val divMulChain: Parser<Expression> = leftAssociative(term, tokenDict.divide or tokenDict.multiply) { a, op, b ->
        if(op.type == tokenDict.multiply) Multiply(a, b) else Divide(a, b)
    }

    val subSumChain = leftAssociative(divMulChain, tokenDict.plus or tokenDict.minus use { type }) { a, op, b ->
        if (op == tokenDict.plus) Add(a, b) else Subtract(a, b)
    }

}