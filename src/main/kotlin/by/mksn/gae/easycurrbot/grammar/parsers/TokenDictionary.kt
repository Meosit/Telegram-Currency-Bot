package by.mksn.gae.easycurrbot.grammar.parsers

import by.mksn.gae.easycurrbot.AppConfig
import by.mksn.gae.easycurrbot.input.CurrencyAliasMatcher
import com.github.h0tk3y.betterParse.grammar.token
import com.github.h0tk3y.betterParse.lexer.Token

/**
 * Container class for the all available expression tokens
 */
class TokenDictionary(config: AppConfig, allCurrenciesRegex: Regex) {

    val number = token(config.strings.tokenNames.number, "(\\d\\s*)+[.,](\\s*\\d)+|(\\d\\s*)*\\d") // greedy whitespace occupation
    val currency = token(config.strings.tokenNames.currency, allCurrenciesRegex)
    // metric suffix must be placed after currency in the token list to support aliases which starts with the one of the suffixes
    val kilo = token(config.strings.tokenNames.kilo, "[кКkK]")
    val mega = token(config.strings.tokenNames.mega, "[мМmM]")

    val whitespace = token(config.strings.tokenNames.whitespace, "\\s+", ignore = true)
    val exclamation = token(config.strings.tokenNames.exclamation, "!")
    val ampersand = token(config.strings.tokenNames.ampersand, "&")

    val leftPar = token(config.strings.tokenNames.leftPar, "\\(")
    val rightPar = token(config.strings.tokenNames.rightPar, "\\)")

    val multiply = token(config.strings.tokenNames.multiply, "\\*")
    val divide = token(config.strings.tokenNames.divide, "/")
    val minus = token(config.strings.tokenNames.minus, "-")
    val plus = token(config.strings.tokenNames.plus, "\\+")

    /**
     * This token is for proper error handling: it placed last and would be captured only of no other (valid) tokens matched.
     * @see InvalidCurrencyFoundException
     */
    val invalidCurrencyToken = token(config.strings.tokenNames.currency, CurrencyAliasMatcher.BROAD_ALIAS_REGEX)

    val allTokens: List<Token> = listOf(
            number, currency,
            kilo, mega,
            whitespace,
            exclamation, ampersand,
            leftPar, rightPar,
            multiply, divide, minus, plus,
            invalidCurrencyToken
    )
}