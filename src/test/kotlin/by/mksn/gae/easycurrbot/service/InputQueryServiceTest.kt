package by.mksn.gae.easycurrbot.service

import by.mksn.gae.easycurrbot.AppConfig
import by.mksn.gae.easycurrbot.entity.InputError
import by.mksn.gae.easycurrbot.entity.Result
import by.mksn.gae.easycurrbot.grammar.InputExpressionGrammar
import com.google.gson.FieldNamingPolicy
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import org.hamcrest.core.Is.`is`
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test


class InputQueryServiceTest {
    companion object {
        private lateinit var service: InputQueryService

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
            val config = AppConfig.create("application.conf")
            exchanger = ExchangeRateService(httpClient, config)
            service = InputQueryService(config, exchanger)
        }
    }

    @Test
    fun `parse normal query`() {
        val input = "12012.12"
        val res = service.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("BYN")))
        assertThat(res.expression, `is`("12012.12"))
        assertThat(res.expressionResult, `is`("12012.12000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB")))
    }


    @Test
    fun `parse expression query`() {
        val input = "3-(2*3)+1"
        val res = service.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("BYN")))
        assertThat(res.expression, `is`("3 - (2*3) + 1"))
        assertThat(res.expressionResult, `is`("-2.00000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB")))
    }

    @Test
    fun `parse expression query 2`() {
        val input = "0,1+0,2"
        val res = service.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("BYN")))
        assertThat(res.expression, `is`("0.1 + 0.2"))
        assertThat(res.expressionResult, `is`("0.30000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB")))
    }

    @Test
    fun `parse expression query 3`() {
        val input = "0,1 + 0,2"
        val res = service.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("BYN")))
        assertThat(res.expression, `is`("0.1 + 0.2"))
        assertThat(res.expressionResult, `is`("0.30000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB")))
    }

    @Test
    fun `parse expression query 4`() {
        val input = "(0,1 + 0,2) / (2 * 2) +CZK"
        val res = service.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("BYN")))
        assertThat(res.expression, `is`("(0.1 + 0.2)/(2*2)"))
        assertThat(res.expressionResult, `is`("0.07500000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB", "CZK")))
    }

    @Test
    fun `parse expression query 5`() {
        val input = "2+7--7 +CZK"
        val res = service.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("BYN")))
        assertThat(res.expression, `is`("2 + 7 - (-7)"))
        assertThat(res.expressionResult, `is`(exchanger.exchangeToApiBase(16.toBigDecimal(), "BYN")))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB", "CZK")))
    }

    @Test
    fun `parse expression query 6`() {
        val input = "-7+2+7 +CZK"
        val res = service.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("BYN")))
        assertThat(res.expression, `is`("(-7) + 2 + 7"))
        assertThat(res.expressionResult, `is`(exchanger.exchangeToApiBase(2.toBigDecimal(), "BYN")))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB", "CZK")))
    }

    @Test
    fun `parse expression query 7`() {
        val input = "2+7* - 2 +CZK"
        val res = service.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("BYN")))
        assertThat(res.expression, `is`("2 + 7*(-2)"))
        assertThat(res.expressionResult, `is`(exchanger.exchangeToApiBase((-12).toBigDecimal(), "BYN")))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB", "CZK")))
    }


    @Test
    fun `parse multi currency expression query 1`() {
        val input = "10 USD + 5 USD +CZK"
        val res = service.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("USD")))
        assertThat(res.expression, `is`("10 USD + 5 USD"))
        assertThat(res.expressionResult, `is`(exchanger.exchangeToApiBase(15.toBigDecimal(), "USD")))
        assertThat(res.targets, `is`(listOf("USD", "BYN", "EUR", "RUB", "CZK")))
    }

    @Test
    fun `parse multi currency expression query 2`() {
        val input = "(2 + 7 )USD + 2EUR +CZK"
        val res = service.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("USD", "EUR")))
        assertThat(res.expression, `is`("(2 + 7) USD + 2 EUR"))
        assertThat(res.expressionResult, `is`(
                exchanger.exchangeToApiBase(9.toBigDecimal(), "USD") +
                exchanger.exchangeToApiBase(2.toBigDecimal(), "EUR")
        ))
        assertThat(res.targets, `is`(listOf("USD", "EUR", "BYN", "RUB", "CZK")))
    }

    @Test
    fun `parse multi currency expression query 3`() {
        val input = "(2 + 7 )USD * 2 + 2EUR/2 +CZK"
        val res = service.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("USD", "EUR")))
        assertThat(res.expression, `is`("(2 + 7) USD*2 + 2 EUR/2"))
        assertThat(res.expressionResult, `is`(
                exchanger.exchangeToApiBase(18.toBigDecimal(), "USD") +
                exchanger.exchangeToApiBase(1.toBigDecimal(), "EUR")
        ))
        assertThat(res.targets, `is`(listOf("USD", "EUR", "BYN", "RUB", "CZK")))
    }

    @Test
    fun `parse invalid value query`() {
        val input = "asd?/"
        val res = service.parse(input)
        assertTrue(res is Result.Failure<InputError>)
        println(res.component2()!!.toMarkdown())
    }


    @Test
    fun `parse invalid expression query 1`() {
        val input = "/*123 -BYN"
        val res = service.parse(input)
        assertTrue(res is Result.Failure<InputError>)
        println(res.component2()!!.toMarkdown())
    }


    @Test
    fun `parse invalid expression value query 2`() {
        val input = "%23"
        val res = service.parse(input)
        assertTrue(res is Result.Failure<InputError>)
        println(res.component2()!!.toMarkdown())
    }

    @Test
    fun `parse invalid expression value query 3`() {
        val input = "123^^"
        val res = service.parse(input)
        assertTrue(res is Result.Failure<InputError>)
        println(res.component2()!!.toMarkdown())
    }


    @Test
    fun `parse invalid expression value query 4`() {
        val input = "123/0"
        val res = service.parse(input)
        assertTrue(res is Result.Failure<InputError>)
        println(res.component2()!!.toMarkdown())
    }


    @Test
    fun `parse invalid multi currency expression query 1`() {
        val input = "123EUR / 123 USD"
        val res = service.parse(input)
        assertTrue(res is Result.Failure<InputError>)
        println(res.component2()!!.toMarkdown())
    }


    @Test
    fun `parse invalid multi currency expression query 2`() {
        val input = "123EUR / 0 + 23 USD"
        val res = service.parse(input)
        assertTrue(res is Result.Failure<InputError>)
        println(res.component2()!!.toMarkdown())
    }


    @Test
    fun `parse invalid multi currency expression query 3`() {
        val input = "123 && EUR / 0 + 23 USD"
        val res = service.parse(input)
        assertTrue(res is Result.Failure<InputError>)
        println(res.component2()!!.toMarkdown())
    }

    @Test
    fun `parse invalid multi currency expression query 4`() {
        val input = "123 EUR EUR"
        val res = service.parse(input)
        assertTrue(res is Result.Failure<InputError>)
        println(res.component2()!!.toMarkdown())
    }


    @Test
    fun `parse query with other base`() {
        val input = "18$"
        val res = service.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("USD")))
        assertThat(res.expression, `is`("18"))
        assertThat(res.expressionResult, `is`(exchanger.exchangeToApiBase(18.toBigDecimal(), "USD")))
        assertThat(res.targets, `is`(listOf("USD", "BYN", "EUR", "RUB")))
    }

    @Test
    fun `parse query with additions`() {
        val input = "18 +кроны +br"
        val res = service.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("BYN")))
        assertThat(res.expressionResult, `is`(exchanger.exchangeToApiBase(18.toBigDecimal(), "BYN")))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB", "CZK")))
    }

    @Test
    fun `parse query with removals`() {
        val input = "18 -br -евро"
        val res = service.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("BYN")))
        assertThat(res.expressionResult, `is`(exchanger.exchangeToApiBase(18.toBigDecimal(), "BYN")))
        assertThat(res.targets, `is`(listOf("USD", "RUB")))
    }

    @Test
    fun `parse query with additions and removals`() {
        val input = "18 +br -br +евро +BYN -USD +злотые"
        val res = service.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("BYN")))
        assertThat(res.expressionResult, `is`(exchanger.exchangeToApiBase(18.toBigDecimal(), "BYN")))
        assertThat(res.targets, `is`(listOf("EUR", "RUB", "PLN")))
    }


    @Test
    fun `parse query non-default base`() {
        val input = "18грн -р"
        val res = service.parse(input).get()
        assertThat(res.involvedCurrencies, `is`(listOf("UAH")))
        assertThat(res.expressionResult, `is`(exchanger.exchangeToApiBase(18.toBigDecimal(), "UAH")))
        assertThat(res.targets, `is`(listOf("UAH", "BYN", "USD", "EUR")))
    }



}