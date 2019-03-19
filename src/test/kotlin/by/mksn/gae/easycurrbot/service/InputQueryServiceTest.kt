package by.mksn.gae.easycurrbot.service

import by.mksn.gae.easycurrbot.config.CurrenciesConfig
import org.hamcrest.core.Is.`is`
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

/**
 * @author Mikhail Snitavets
 */
class InputQueryServiceTest {
    private lateinit var service: InputQueryService

    @Before
    fun setUp() {
        val currConf = CurrenciesConfig.create("currencies.conf")
        service = InputQueryService(currConf)
    }

    @Test
    fun `parse normal query`() {
        val input = "12012.12"
        val res = service.parse(input)
        if(res == null) {
            fail()
            return
        }
        assertThat(res.base, `is`("BYN"))
        assertThat(res.value, `is`("12012.12".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB")))

    }


    @Test
    fun `parse expression query`() {
        val input = "3-(2*3)+1"
        val res = service.parse(input)
        if(res == null) {
            fail()
            return
        }
        assertThat(res.base, `is`("BYN"))
        assertThat(res.value, `is`("2".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB")))
    }


    @Test
    fun `parse query with other base`() {
        val input = "18$"
        val res = service.parse(input)
        if(res == null) {
            fail()
            return
        }
        assertThat(res.base, `is`("USD"))
        assertThat(res.value, `is`("18".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("USD", "BYN", "EUR", "RUB")))
    }

    @Test
    fun `parse query with additions`() {
        val input = "18 +nonmatch +кроны "
        val res = service.parse(input)
        if(res == null) {
            fail()
            return
        }
        assertThat(res.base, `is`("BYN"))
        assertThat(res.value, `is`("18".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("BYN", "USD", "EUR", "RUB", "CZK")))
    }

    @Test
    fun `parse query with removals`() {
        val input = "18 -br -евро"
        val res = service.parse(input)
        if(res == null) {
            fail()
            return
        }
        assertThat(res.base, `is`("BYN"))
        assertThat(res.value, `is`("18".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("USD", "RUB")))
    }

    @Test
    fun `parse query with additions and removals`() {
        val input = "18 +br -br +евро +BYN -USD +злотые"
        val res = service.parse(input)
        if(res == null) {
            fail()
            return
        }
        assertThat(res.base, `is`("BYN"))
        assertThat(res.value, `is`("18".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("EUR", "RUB", "PLN")))
    }


    @Test
    fun `parse query non-default base`() {
        val input = "18грн -р"
        val res = service.parse(input)
        if(res == null) {
            fail()
            return
        }
        assertThat(res.base, `is`("UAH"))
        assertThat(res.value, `is`("18".toBigDecimal()))
        assertThat(res.targets, `is`(listOf("UAH", "BYN", "USD", "EUR")))
    }



}