package by.mksn.gae.easycurrbot.service

import by.mksn.gae.easycurrbot.config.CurrenciesConfig
import com.google.gson.annotations.SerializedName
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

/**
 * @author Mikhail Snitavets
 */
class ExchangeRateService(private val httpClient: HttpClient, private val config: CurrenciesConfig) {

    companion object {
        val LOG = LoggerFactory.getLogger(ExchangeRateService::class.java)!!
    }

    private var previousUpdateDate: LocalDateTime = LocalDateTime.parse("1970-01-01T01:01:01")
    private lateinit var exchangeRates: Map<String, BigDecimal>

    init {
        invalidateExchangeRates()
    }

    @Synchronized
    private fun invalidateExchangeRates() {
        val hours = Duration.between(previousUpdateDate, LocalDateTime.now(ZoneId.of("UTC+3"))).toHours()
        LOG.info("Date difference is $hours hours")
        if (hours >= 24) {
            LOG.info("Reloading exchange rates...")
            val rawExchangeRates = runBlocking { httpClient.get<List<RawExchangeRate>>(config.apiUrl) }
            exchangeRates = rawExchangeRates.asSequence()
                    .filter { rate -> rate.currencyRate != null }
                    .filter { rate -> config.currencies.supported.any { it.code == rate.currencyAbbreviation } }
                    .associateBy(
                            { it.currencyAbbreviation },
                            { it.currencyRate!! / it.currencyScale.toBigDecimal() }
                    )
            exchangeRates = exchangeRates + (config.currencies.base to 1.toBigDecimal())
            LOG.info("Loaded ${exchangeRates.size} rates")
            LOG.info(exchangeRates.map { "${it.key} -> ${it.value}" }.joinToString(separator = "\n"))
            previousUpdateDate = LocalDateTime.parse(rawExchangeRates.first().exchangeDate)
        }
    }

    fun exchange(value: BigDecimal, base: String, targets: List<String>): Map<String, BigDecimal> {
        invalidateExchangeRates()
        return targets.associateWith { (value.toApiBaseValue(base) / exchangeRates.getValue(it)) }
    }


    private fun BigDecimal.toApiBaseValue(base: String) =
            if (base == config.currencies.base) {
                this
            } else {
                val rate = exchangeRates[base]
                        ?: throw IllegalArgumentException("Unknown base currency provided ($base)")
                this * rate
            }

}

private data class RawExchangeRate(
        @SerializedName("Date") val exchangeDate: String,
        @SerializedName("Cur_ID") val currencyId: Int,
        @SerializedName("Cur_Abbreviation") val currencyAbbreviation: String,
        @SerializedName("Cur_Scale") val currencyScale: Int,
        @SerializedName("Cur_Name") val currencyName: String,
        @SerializedName("Cur_OfficialRate") val currencyRate: BigDecimal?
)
