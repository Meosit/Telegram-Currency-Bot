package by.mksn.gae.easycurrbot.input

import by.mksn.gae.easycurrbot.AppConfig
import by.mksn.gae.easycurrbot.exchange.ExchangeRateService
import by.mksn.gae.easycurrbot.grammar.expression.ExpressionType
import by.mksn.gae.easycurrbot.util.Result
import com.google.gson.FieldNamingPolicy
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import org.hamcrest.core.Is.`is`
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test


class InputQueryParserTest {
    companion object {
        private lateinit var config: AppConfig

        private lateinit var queryParser: InputQueryParser

        private lateinit var exchanger: ExchangeRateService

        @BeforeClass @JvmStatic
        fun setUp() {
            val httpClient = HttpClient(Apache) {
                install(JsonFeature) {
                    serializer = GsonSerializer {
                        setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    }
                }
            }
            config = AppConfig.create("application.conf")
            exchanger = ExchangeRateService(httpClient, config)
            queryParser = InputQueryParser(config, exchanger)
        }
    }

    @Test
    fun `(positive) decimal value`() {
        val input = "12012.12"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("BYN")))
        assertThat(res.baseCurrency, `is`("BYN"))
        assertThat(res.expression, `is`("12012.12"))
        assertThat(res.type, `is`(ExpressionType.SINGLE_VALUE))
        assertThat(res.expressionResult, `is`("12012.12000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB")))
    }

    @Test
    fun `(positive) decimal value with EXPLICIT api base currency without spaces`() {
        val input = "12012.12BYN"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("BYN")))
        assertThat(res.baseCurrency, `is`("BYN"))
        assertThat(res.expression, `is`("12012.12"))
        assertThat(res.type, `is`(ExpressionType.SINGLE_VALUE))
        assertThat(res.expressionResult, `is`("12012.12000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB")))
    }

    @Test
    fun `(positive) simple number with KILO suffix`() {
        val input = "10k"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("BYN")))
        assertThat(res.baseCurrency, `is`("BYN"))
        assertThat(res.expression, `is`("10000"))
        assertThat(res.type, `is`(ExpressionType.SINGLE_VALUE))
        assertThat(res.expressionResult, `is`("10000.00000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB")))
    }

    @Test
    fun `(positive) simple number with MEGA suffix`() {
        val input = "1m"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("BYN")))
        assertThat(res.baseCurrency, `is`("BYN"))
        assertThat(res.expression, `is`("1000000"))
        assertThat(res.type, `is`(ExpressionType.SINGLE_VALUE))
        assertThat(res.expressionResult, `is`("1000000.00000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB")))
    }

    @Test
    fun `(positive) simple number with DOUBLE KILO suffix and explicit currency WITHOUT SPACES`() {
        val input = "1kkBYN"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("BYN")))
        assertThat(res.baseCurrency, `is`("BYN"))
        assertThat(res.expression, `is`("1000000"))
        assertThat(res.type, `is`(ExpressionType.SINGLE_VALUE))
        assertThat(res.expressionResult, `is`("1000000.00000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB")))
    }

    @Test
    fun `(positive) simple number with mega suffix and explicit currency SEPARATED BY SPACES`() {
        val input = "1 M BYN"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("BYN")))
        assertThat(res.baseCurrency, `is`("BYN"))
        assertThat(res.expression, `is`("1000000"))
        assertThat(res.type, `is`(ExpressionType.SINGLE_VALUE))
        assertThat(res.expressionResult, `is`("1000000.00000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB")))
    }

    @Test
    fun `(positive) simple number with kilo suffix and 'k'-STARTED currency WITHOUT spaces`() {
        val input = "1kkc"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("CZK")))
        assertThat(res.baseCurrency, `is`("CZK"))
        assertThat(res.type, `is`(ExpressionType.SINGLE_VALUE))
        assertThat(res.expression, `is`("1000"))
        assertThat(res.expressionResult, `is`("1000.00000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB", "CZK")))
    }

    @Test
    fun `(positive) simple number with RUSSIAN currency alias and kilo suffix without spaces`() {
        val input = "1кр"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("RUB")))
        assertThat(res.baseCurrency, `is`("RUB"))
        assertThat(res.type, `is`(ExpressionType.SINGLE_VALUE))
        assertThat(res.expression, `is`("1000"))
        assertThat(res.expressionResult, `is`("1000.00000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB")))
    }


    @Test
    fun `(positive) multi-currency query, kilo suffixes and aliases WITHOUT SPACES in-between them`() {
        val input = "1kkc + 10k$"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("CZK", "USD")))
        assertThat(res.baseCurrency, `is`("BYN"))
        assertThat(res.type, `is`(ExpressionType.MULTI_CURRENCY_EXPR))
        assertThat(res.expression, `is`("1000 CZK + 10000 USD"))
        assertThat(res.expressionResult, `is`(exchanger.exchangeToApiBase(1000.toBigDecimal(), "CZK") +
                exchanger.exchangeToApiBase(10000.toBigDecimal(), "USD")))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB", "CZK")))
    }


    @Test
    fun `(positive) multi-currency query, kilo suffixes and aliases WITH SPACES in-between them`() {
        val input = "1k BYN + 10k BYN"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("BYN")))
        assertThat(res.baseCurrency, `is`("BYN"))
        assertThat(res.type, `is`(ExpressionType.SINGLE_CURRENCY_EXPR))
        assertThat(res.expression, `is`("1000 + 10000"))
        assertThat(res.expressionResult, `is`(exchanger.exchangeToApiBase(11000.toBigDecimal(), "BYN")))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB")))
    }

    @Test
    fun `(positive) number with spaces, currency alias with INCORRECT KEYBOARD layout`() {
        val input = "100 000 ,h"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("BYN")))
        assertThat(res.baseCurrency, `is`("BYN"))
        assertThat(res.expression, `is`("100000"))
        assertThat(res.type, `is`(ExpressionType.SINGLE_VALUE))
        assertThat(res.expressionResult, `is`("100000.00000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB")))
    }


    @Test
    fun `(negative) kilo suffix in the middle of the number`() {
        val input = "100k 000 ,h"
        val res = queryParser.parse(input) as Result.Failure<InputError>
        assertThat(res.error.message, `is`(config.strings.errors.unparsedReminder))
        assertThat(res.error.errorPosition, `is`(6))
        println(res.error.toMarkdown())
    }

    @Test
    fun `(positive) decimal value with spaces and alias with INCORRECT KEYBOARD layout `() {
        val input = "12012.12 ,h"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("BYN")))
        assertThat(res.baseCurrency, `is`("BYN"))
        assertThat(res.expression, `is`("12012.12"))
        assertThat(res.expressionResult, `is`("12012.12000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB")))
    }


    @Test
    fun `(positive) simple number with spaces and alias with INCORRECT KEYBOARD layout`() {
        val input = "12 he,kz"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("RUB")))
        assertThat(res.baseCurrency, `is`("RUB"))
        assertThat(res.expression, `is`("12"))
        assertThat(res.expressionResult, `is`("12.00000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB")))
    }

    @Test
    fun `(positive) math expression with simple numbers and PARENTHESES`() {
        val input = "3-(2*3)+1"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("BYN")))
        assertThat(res.baseCurrency, `is`("BYN"))
        assertThat(res.expression, `is`("3 - 2*3 + 1"))
        assertThat(res.type, `is`(ExpressionType.SINGLE_CURRENCY_EXPR))
        assertThat(res.expressionResult, `is`("-2.00000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB")))
    }

    @Test
    fun `(positive) math expression with decimal numbers USING COMMAS, WITHOUT SPACES`() {
        val input = "0,1+0,2"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("BYN")))
        assertThat(res.baseCurrency, `is`("BYN"))
        assertThat(res.expression, `is`("0.1 + 0.2"))
        assertThat(res.type, `is`(ExpressionType.SINGLE_CURRENCY_EXPR))
        assertThat(res.expressionResult, `is`("0.30000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB")))
    }

    @Test
    fun `(positive) math expression with decimal numbers USING COMMAS, WITH SPACES`() {
        val input = "0,1 + 0,2"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("BYN")))
        assertThat(res.expression, `is`("0.1 + 0.2"))
        assertThat(res.type, `is`(ExpressionType.SINGLE_CURRENCY_EXPR))
        assertThat(res.expressionResult, `is`("0.30000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB")))
    }

    @Test
    fun `(positive) math expression with decimal numbers, spaces, parentheses and ADDITIONAL CURRENCY key`() {
        val input = "(0,1 + 0,2) / (2 * 2) !CZK"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("BYN")))
        assertThat(res.baseCurrency, `is`("BYN"))
        assertThat(res.expression, `is`("(0.1 + 0.2)/(2*2)"))
        assertThat(res.type, `is`(ExpressionType.SINGLE_CURRENCY_EXPR))
        assertThat(res.expressionResult, `is`("0.07500000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB", "CZK")))
    }

    @Test
    fun `(positive) math expression with NEGATION AFTER SUBTRACT operator, without spaces`() {
        val input = "2+7--7 +CZK"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("BYN")))
        assertThat(res.baseCurrency, `is`("BYN"))
        assertThat(res.expression, `is`("2 + 7 - (-7)"))
        assertThat(res.type, `is`(ExpressionType.SINGLE_CURRENCY_EXPR))
        assertThat(res.expressionResult, `is`(exchanger.exchangeToApiBase(16.toBigDecimal(), "BYN")))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB", "CZK")))
    }

    @Test
    fun `(positive) math expression with NEGATION AS FIRST argument`() {
        val input = "-7+2+7 !CZK"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("BYN")))
        assertThat(res.baseCurrency, `is`("BYN"))
        assertThat(res.expression, `is`("-7 + 2 + 7"))
        assertThat(res.type, `is`(ExpressionType.SINGLE_CURRENCY_EXPR))
        assertThat(res.expressionResult, `is`(exchanger.exchangeToApiBase(2.toBigDecimal(), "BYN")))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB", "CZK")))
    }

    @Test
    fun `(positive) math expression with NEGATION AFTER MULTIPLY operator, with spaces`() {
        val input = "2+7* - 2 !CZK"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("BYN")))
        assertThat(res.expression, `is`("2 + 7*(-2)"))
        assertThat(res.type, `is`(ExpressionType.SINGLE_CURRENCY_EXPR))
        assertThat(res.expressionResult, `is`(exchanger.exchangeToApiBase((-12).toBigDecimal(), "BYN")))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB", "CZK")))
    }


    @Test
    fun `(positive) simple multi-qurrency query, with spaces and additional currency key`() {
        val input = "10 USD + 5 USD +CZK"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("USD")))
        assertThat(res.baseCurrency, `is`("USD"))
        assertThat(res.expression, `is`("10 + 5"))
        assertThat(res.type, `is`(ExpressionType.SINGLE_CURRENCY_EXPR))
        assertThat(res.expressionResult, `is`("15.00000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB", "CZK")))
    }

    @Test
    fun `(positive) multi-currency expression with MATH EXPRESSION as currencied value`() {
        val input = "(2 + 7 )USD + 2EUR +CZK"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("USD", "EUR")))
        assertThat(res.expression, `is`("(2 + 7) USD + 2 EUR"))
        assertThat(res.type, `is`(ExpressionType.MULTI_CURRENCY_EXPR))
        assertThat(res.expressionResult, `is`(
                exchanger.exchangeToApiBase(9.toBigDecimal(), "USD") +
                exchanger.exchangeToApiBase(2.toBigDecimal(), "EUR")
        ))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB", "CZK")))
    }

    @Test
    fun `(positive) multi-currency expression with DIVISION of currencied value`() {
        val input = "(2 + 7 )USD * 2 + 2EUR/2 &CZK"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("USD", "EUR")))
        assertThat(res.expression, `is`("(2 + 7) USD*2 + 2 EUR/2"))
        assertThat(res.type, `is`(ExpressionType.MULTI_CURRENCY_EXPR))
        assertThat(res.expressionResult, `is`(
                exchanger.exchangeToApiBase(18.toBigDecimal(), "USD") +
                exchanger.exchangeToApiBase(1.toBigDecimal(), "EUR")
        ))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB", "CZK")))
    }


    @Test
    fun `(positive) multi-currency expression with PREFIXED NOTATION of currencies `() {
        val input = "USD(2 + 7 ) * 2 + EUR 2 &CZK"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("USD", "EUR")))
        assertThat(res.expression, `is`("(2 + 7)*2 USD + 2 EUR"))
        assertThat(res.type, `is`(ExpressionType.MULTI_CURRENCY_EXPR))
        assertThat(res.expressionResult, `is`(
                exchanger.exchangeToApiBase(18.toBigDecimal(), "USD") +
                exchanger.exchangeToApiBase(2.toBigDecimal(), "EUR")
        ))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB", "CZK")))
    }

    @Test
    fun `(positive) multi-currency expression with DIVISION WITHOUT PARENTHESES as currencied value `() {
        val input = "(2 + 7 )USD * 2 + 2/2 EUR &CZK"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("USD", "EUR")))
        assertThat(res.expression, `is`("(2 + 7) USD*2 + 2/2 EUR"))
        assertThat(res.type, `is`(ExpressionType.MULTI_CURRENCY_EXPR))
        assertThat(res.expressionResult, `is`(
                exchanger.exchangeToApiBase(18.toBigDecimal(), "USD") +
                exchanger.exchangeToApiBase(1.toBigDecimal(), "EUR")
        ))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB", "CZK")))
    }

    @Test
    fun `(positive) math expression with CURRENCY IN THE MIDDLE`() {
        val input = "1000$ * 0.91 / 10"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("USD")))
        assertThat(res.type, `is`(ExpressionType.SINGLE_CURRENCY_EXPR))
        assertThat(res.expression, `is`("1000*0.91/10"))
        assertThat(res.expressionResult, `is`("91.00000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB")))
    }

    @Test
    fun `(positive) math expression with CURRENCY IN THE MIDDLE and multiplication after division (evaluation priority)`() {
        val input = "1000$ * 0.91 / 10 * 10"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("USD")))
        assertThat(res.baseCurrency, `is`("USD"))
        assertThat(res.type, `is`(ExpressionType.SINGLE_CURRENCY_EXPR))
        assertThat(res.expression, `is`("1000*0.91/10*10"))
        assertThat(res.expressionResult, `is`("910.00000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB")))
    }

    @Test
    fun `(positive) multi-currency expression with alias of INCORRECT CASE`() {
        val input = "10 * 10 * 10 eUrO + 10 USD"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("EUR", "USD")))
        assertThat(res.expression, `is`("10*10*10 EUR + 10 USD"))
        assertThat(res.expressionResult, `is`(exchanger.exchangeToApiBase(1000.toBigDecimal(), "EUR") +
                exchanger.exchangeToApiBase(10.toBigDecimal(), "USD")))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB")))
    }

    @Test
    fun `(positive) math expression with currency in the middle and PARENTHESES (evaluation priority)`() {
        val input = "(1000$ * 0.91) / 10 * 10"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("USD")))
        assertThat(res.baseCurrency, `is`("USD"))
        assertThat(res.type, `is`(ExpressionType.SINGLE_CURRENCY_EXPR))
        assertThat(res.expressionResult, `is`("910.00000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB")))
    }

    @Test
    fun `(negative) invalid '^^' operator at end `() {
        val input = "123^^"
        val res = queryParser.parse(input) as Result.Failure<InputError>
        assertThat(res.error.message, `is`(config.strings.errors.unparsedReminder))
        assertThat(res.error.errorPosition, `is`(4))
        println(res.error.toMarkdown())
    }

    @Test
    fun `(negative) invalid alias at start`() {
        val input = "asd?/"
        val res = queryParser.parse(input) as Result.Failure<InputError>
        assertThat(res.error.message, `is`(config.strings.errors.invalidCurrencyAlias.format("asd")))
        assertThat(res.error.errorPosition, `is`(1))
        println(res.error.toMarkdown())
    }

    @Test
    fun `(negative) invalid '%' operator at start`() {
        val input = "%23"
        val res = queryParser.parse(input) as Result.Failure<InputError>
        assertThat(res.error.message, `is`(config.strings.errors.invalidCurrencyAlias.format("%23")))
        assertThat(res.error.errorPosition, `is`(1))
        println(res.error.toMarkdown())
    }

    @Test
    fun `(negative) division operator at start`() {
        val input = "/*123 &BYN"
        val res = queryParser.parse(input) as Result.Failure<InputError>
        assertThat(res.error.message, `is`(config.strings.errors.mismatchedToken.format("/", "код валюты")))
        assertThat(res.error.errorPosition, `is`(1))
        println(res.error.toMarkdown())
    }

    @Test
    fun `(negative) multiply operator at start`() {
        val input = "*1000"
        val res = queryParser.parse(input)
        assertTrue(res is Result.Failure<InputError>)
        assertThat(res.component2()!!.errorPosition, `is`(1))
        println(res.component2()!!.toMarkdown())
    }

    @Test
    fun `(negative) division by zero`() {
        val input = "123/0"
        val res = queryParser.parse(input) as Result.Failure<InputError>
        assertThat(res.error.message, `is`(config.strings.errors.divisionByZero))
        assertThat(res.error.errorPosition, `is`(1))
        println(res.error.toMarkdown())
    }

    @Test
    fun `(negative) division by zero with additional currency key`() {
        val input = "10 / 0 +долларов"
        val res = queryParser.parse(input) as Result.Failure<InputError>
        assertThat(res.error.message, `is`(config.strings.errors.divisionByZero))
        assertThat(res.error.errorPosition, `is`(1))
        println(res.error.toMarkdown())
    }

    @Test
    fun `(negative) division by zero in multi-currency expression`() {
        val input = "123EUR / 0 + 23 USD"
        val res = queryParser.parse(input) as Result.Failure<InputError>
        assertThat(res.error.message, `is`(config.strings.errors.divisionByZero))
        assertThat(res.error.errorPosition, `is`(1))
        println(res.error.toMarkdown())
    }

    @Test
    fun `(negative) division by zero in multi-currency expression with aliases`() {
        val input = "10 долларов + 10 евро / 0"
        val res = queryParser.parse(input)
        assertTrue(res is Result.Failure<InputError>)
        assertThat(res.component2()!!.errorPosition, `is`(1))
        println(res.component2()!!.toMarkdown())
    }

    @Test
    fun `(negative) division by currencied number`() {
        val input = "123EUR / 123 USD"
        val res = queryParser.parse(input) as Result.Failure<InputError>
        assertThat(res.error.message, `is`(config.strings.errors.illegalCurrencyPlacement))
        assertThat(res.error.errorPosition, `is`(14))
        println(res.error.toMarkdown())
    }


    @Test
    fun `(negative) invalid decimal part delimiter`() {
        val input = "1.9к$ * 0ю91euro"
        val res = queryParser.parse(input) as Result.Failure<InputError>
        assertThat(res.error.message, `is`(config.strings.errors.illegalCurrencyPlacement))
        assertThat(res.error.errorPosition, `is`(10))
        println(res.error.toMarkdown())
    }

    @Test
    fun `(negative) invalid operator '&&' with several errors after`() {
        val input = "123 && EUR / 0 + 23 USD"
        val res = queryParser.parse(input) as Result.Failure<InputError>
        assertThat(res.error.message, `is`(config.strings.errors.unparsedReminder))
        assertThat(res.error.errorPosition, `is`(5))
        println(res.error.toMarkdown())
    }

    @Test
    fun `(negative) two consequent currency definitions`() {
        val input = "123 EUR EUR"
        val res = queryParser.parse(input) as Result.Failure<InputError>
        assertThat(res.error.message, `is`(config.strings.errors.illegalCurrencyPlacement))
        assertThat(res.error.errorPosition, `is`(9))
        println(res.error.toMarkdown())
    }

    @Test
    fun `(negative) invalid alias in multi-currency expression`() {
        val input = "10 долларов + 10 asdf"
        val res = queryParser.parse(input) as Result.Failure<InputError>
        assertThat(res.error.message, `is`(config.strings.errors.invalidCurrencyAlias.format("asdf")))
        assertThat(res.error.errorPosition, `is`(18))
        println(res.error.toMarkdown())
    }

    @Test
    fun `(positive) simple number with explicit API NON-BASE currency, without spaces`() {
        val input = "18$"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("USD")))
        assertThat(res.expression, `is`("18"))
        assertThat(res.baseCurrency, `is`("USD"))
        assertThat(res.type, `is`(ExpressionType.SINGLE_VALUE))
        assertThat(res.expressionResult, `is`("18.00000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB")))
    }

    @Test
    fun `(positive) simple number with explicit API NON-BASE currency, with spaces`() {
        val input = "1 UAH"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("UAH")))
        assertThat(res.expression, `is`("1"))
        assertThat(res.baseCurrency, `is`("UAH"))
        assertThat(res.type, `is`(ExpressionType.SINGLE_VALUE))
        assertThat(res.expressionResult, `is`("1.00000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB", "UAH")))
    }

    @Test
    fun `(positive) simple number with explicit API NON-BASE currency and additional currency key using differen keyboard layout `() {
        val input = "18грн !aeyns"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("UAH")))
        assertThat(res.baseCurrency, `is`("UAH"))
        assertThat(res.type, `is`(ExpressionType.SINGLE_VALUE))
        assertThat(res.expressionResult, `is`("18.00000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB", "UAH", "GBP")))
    }

    @Test
    fun `(positive) simple number with additional currency keys using different key symbols`() {
        val input = "18 +кроны !br"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("BYN")))
        assertThat(res.baseCurrency, `is`("BYN"))
        assertThat(res.type, `is`(ExpressionType.SINGLE_VALUE))
        assertThat(res.expressionResult, `is`(exchanger.exchangeToApiBase(18.toBigDecimal(), "BYN")))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB", "CZK")))
    }

    @Test
    fun `(positive) currency-only query`() {
        val input = "Гривна"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("UAH")))
        assertThat(res.baseCurrency, `is`("UAH"))
        assertThat(res.type, `is`(ExpressionType.SINGLE_VALUE))
        assertThat(res.expressionResult, `is`("1.00000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB", "UAH")))
    }

    @Test
    fun `(positive) currency-only query with additional currency key`() {
        val input = "рубль &гривна"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("RUB")))
        assertThat(res.baseCurrency, `is`("RUB"))
        assertThat(res.type, `is`(ExpressionType.SINGLE_VALUE))
        assertThat(res.expressionResult, `is`("1.00000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB", "UAH")))
    }


    @Test
    fun `(negative) currency-only query with illegal char at the end`() {
        val input = "доллар ?"
        val res = queryParser.parse(input) as Result.Failure<InputError>
        assertThat(res.error.message, `is`(config.strings.errors.unparsedReminder))
        assertThat(res.error.errorPosition, `is`(8))
        println(res.error.toMarkdown())
    }

    @Test
    fun `(negative) currency-only query with invalid alias`() {
        val input = "asdf"
        val res = queryParser.parse(input) as Result.Failure<InputError>
        assertThat(res.error.message, `is`(config.strings.errors.invalidCurrencyAlias.format("asdf")))
        assertThat(res.error.errorPosition, `is`(1))
        println(res.error.toMarkdown())
    }

    @Test
    fun `(negative) invalid alias which contains the valid one`() {
        val input = "1 usdd"
        val res = queryParser.parse(input) as Result.Failure<InputError>
        assertThat(res.error.message, `is`(config.strings.errors.unparsedReminder))
        assertThat(res.error.errorPosition, `is`(6))
        println(res.error.toMarkdown())
    }

    @Test
    fun `(positive) very long number with prefix currency notation`() {
        val input = "usd 488328938372887977341537259497997851352671159292899697236058208809454048246899111241332161343881238402187713643910538138490086922551030374059966793632190643617540775466354136146108018361168082820587948041800957124719210860435589413028616075788651235472"
        val res = queryParser.parse(input)
        assertTrue(res is Result.Success<InputQuery>)
    }

    @Test
    fun `(positive) simple value with additional currency key using russian native union`() {
        val input = "18 евро в фунтах"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("EUR")))
        assertThat(res.baseCurrency, `is`("EUR"))
        assertThat(res.type, `is`(ExpressionType.SINGLE_VALUE))
        assertThat(res.expressionResult, `is`("18.00000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB", "GBP")))
    }

    @Test
    fun `(positive) simple value with additional currency key using english native union`() {
        val input = "18 euro in pounds"
        val res = queryParser.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("EUR")))
        assertThat(res.baseCurrency, `is`("EUR"))
        assertThat(res.type, `is`(ExpressionType.SINGLE_VALUE))
        assertThat(res.expressionResult, `is`("18.00000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB", "GBP")))
    }


    @Test
    fun `(negative) russian native union concatenated with currency`() {
        val input = "18 euroinpounds"
        val res = queryParser.parse(input) as Result.Failure<InputError>
        assertThat(res.error.message, `is`(config.strings.errors.unparsedReminder))
        assertThat(res.error.errorPosition, `is`(8))
        println(res.error.toMarkdown())
    }

}
