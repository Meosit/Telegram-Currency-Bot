package by.mksn.gae.easycurrbot.service

import by.mksn.gae.easycurrbot.AppConfig
import by.mksn.gae.easycurrbot.entity.Result
import org.hamcrest.core.Is.`is`
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test


class InputQueryServiceTest {
    private lateinit var service: InputQueryService

    @Before
    fun setUp() {
        val conf = AppConfig.create("application.conf")
        service = InputQueryService(conf)
    }

    @Test
    fun `parse normal query`() {
        val input = "12012.12"
        val res = service.parse(input).get()
        assertThat(res.base, `is`("BYN"))
        assertThat(res.sumExpression, `is`("12012.12"))
        assertThat(res.sum, `is`("12012.12000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB")))

    }


    @Test
    fun `parse expression query`() {
        val input = "3-(2*3)+1"
        val res = service.parse(input).get()
        assertThat(res.base, `is`("BYN"))
        assertThat(res.sumExpression, `is`("3 - (2*3) + 1"))
        assertThat(res.sum, `is`("2.00000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB")))
    }

    @Test
    fun `parse expression query 2`() {
        val input = "0,1+0,2"
        val res = service.parse(input).get()
        assertThat(res.base, `is`("BYN"))
        assertThat(res.sumExpression, `is`("0.1 + 0.2"))
        assertThat(res.sum, `is`("0.30000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB")))
    }

    @Test
    fun `parse expression query 3`() {
        val input = "0,1 + 0,2"
        val res = service.parse(input).get()
        assertThat(res.base, `is`("BYN"))
        assertThat(res.sumExpression, `is`("0.1 + 0.2"))
        assertThat(res.sum, `is`("0.30000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB")))
    }

    @Test
    fun `parse expression query 4`() {
        val input = "(0,1 + 0,2) / (2 ^ 2) +CZK"
        val res = service.parse(input).get()
        assertThat(res.base, `is`("BYN"))
        assertThat(res.sumExpression, `is`("(0.1 + 0.2)/(2^2)"))
        assertThat(res.sum, `is`("0.07500000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB", "CZK")))
    }

    @Test
    fun `parse expression query 5`() {
        val input = "2+7--7 +CZK"
        val res = service.parse(input).get()
        assertThat(res.base, `is`("BYN"))
        assertThat(res.sumExpression, `is`("2 + 7 - (-7)"))
        assertThat(res.sum, `is`("16.00000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB", "CZK")))
    }

    @Test
    fun `parse expression query 6`() {
        val input = "-7+2+7 +CZK"
        val res = service.parse(input).get()
        assertThat(res.base, `is`("BYN"))
        assertThat(res.sumExpression, `is`("(-7) + 2 + 7"))
        assertThat(res.sum, `is`("2.00000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB", "CZK")))
    }

    @Test
    fun `parse expression query 7`() {
        val input = "2+7* - 2 +CZK"
        val res = service.parse(input).get()
        assertThat(res.base, `is`("BYN"))
        assertThat(res.sumExpression, `is`("2 + 7*(-2)"))
        assertThat(res.sum, `is`("12.00000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB", "CZK")))
    }

    @Test
    fun `parse invalid value query`() {
        val input = "asd?/"
        val res = service.parse(input)
        assertTrue(res is Result.Failure<String>)
        print(res.component2())
    }


    @Test
    fun `parse invalid expression query`() {
        val input = "/*123 -BYN"
        val res = service.parse(input)
        assertTrue(res is Result.Failure<String>)
        print(res.component2())
    }


    @Test
    fun `parse invalid expression value query 2`() {
        val input = "%23"
        val res = service.parse(input)
        assertTrue(res is Result.Failure<String>)
        print(res.component2())
    }

    @Test
    fun `parse invalid expression value query 3`() {
        val input = "123^^"
        val res = service.parse(input)
        assertTrue(res is Result.Failure<String>)
        print(res.component2())
    }


    @Test
    fun `parse invalid expression value query 4`() {
        val input = "123/0"
        val res = service.parse(input)
        assertTrue(res is Result.Failure<String>)
        print(res.component2())
    }


    @Test
    fun `parse query with other base`() {
        val input = "18$"
        val res = service.parse(input).get()
        assertThat(res.base, `is`("USD"))
        assertThat(res.sumExpression, `is`("18"))
        assertThat(res.sum, `is`("18.00000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("USD", "BYN", "EUR", "RUB")))
    }

    @Test
    fun `parse query with additions`() {
        val input = "18 +кроны +br"
        val res = service.parse(input).get()
        assertThat(res.base, `is`("BYN"))
        assertThat(res.sum, `is`("18.00000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB", "CZK")))
    }

    @Test
    fun `parse query with removals`() {
        val input = "18 -br -евро"
        val res = service.parse(input).get()
        assertThat(res.base, `is`("BYN"))
        assertThat(res.sum, `is`("18.00000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "RUB")))
    }

    @Test
    fun `parse query with additions and removals`() {
        val input = "18 +br -br +евро +BYN -USD +злотые"
        val res = service.parse(input).get()
        assertThat(res.base, `is`("BYN"))
        assertThat(res.sum, `is`("18.00000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "EUR", "RUB", "PLN")))
    }


    @Test
    fun `parse query non-default base`() {
        val input = "18грн -р"
        val res = service.parse(input).get()
        assertThat(res.base, `is`("UAH"))
        assertThat(res.sum, `is`("18.00000000".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("UAH", "BYN", "USD", "EUR")))
    }



}