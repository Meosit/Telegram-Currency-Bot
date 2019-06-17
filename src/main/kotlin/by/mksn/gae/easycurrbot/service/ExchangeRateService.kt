package by.mksn.gae.easycurrbot.service

import by.mksn.gae.easycurrbot.AppConfig
import by.mksn.gae.easycurrbot.entity.*
import com.google.gson.annotations.SerializedName
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

private data class RawExchangeRate(
        @SerializedName("Date") val exchangeDate: String,
        @SerializedName("Cur_ID") val currencyId: Int,
        @SerializedName("Cur_Abbreviation") val currencyAbbreviation: String,
        @SerializedName("Cur_Scale") val currencyScale: Int,
        @SerializedName("Cur_Name") val currencyName: String,
        @SerializedName("Cur_OfficialRate") val currencyRate: BigDecimal?
)

class ExchangeRateService(private val httpClient: HttpClient, private val config: AppConfig) {
    companion object {
        val LOG = LoggerFactory.getLogger(ExchangeRateService::class.java)!!
    }

    private val supportedCurrencies = config.currencies.supported.associateBy { it.code }
    private var previousUpdateDate: LocalDateTime
    private lateinit var exchangeRates: Map<String, BigDecimal>
    private lateinit var exchangeRatesDashboard: Array<ExchangeResults>

    init {
        previousUpdateDate = LocalDateTime.parse("1970-01-01T01:01:01")
        invalidateExchangeRates()
    }

    @Synchronized
    fun invalidateExchangeRates() {
        val now = LocalDateTime.now(ZoneId.of("UTC+3"))
        val hours = Duration.between(previousUpdateDate, now).toHours()
        if (hours >= 24) {
            LOG.info("Reloading exchange rates...")
            val rawExchangeRates = runBlocking { httpClient.get<List<RawExchangeRate>>(config.currencies.apiUrl) }
            exchangeRates = rawExchangeRates.asSequence()
                    .filter { it.currencyRate != null }
                    .filter { supportedCurrencies.containsKey(it.currencyAbbreviation ) }
                    .associateBy(
                            { it.currencyAbbreviation },
                            { it.currencyRate!!.toConfScale(config) / it.currencyScale.toBigDecimal() }
                    )
            exchangeRates = exchangeRates + (config.currencies.apiBase to 1.toConfScaledBigDecimal(config))
            LOG.info("Loaded ${exchangeRates.size} rates:\n"
                    + exchangeRates.map { "${it.key} -> ${it.value}" }.joinToString(separator = "\n"))
            previousUpdateDate = LocalDateTime.parse(rawExchangeRates.first().exchangeDate)

            exchangeRatesDashboard = config.currencies.dashboard.asSequence()
                    .map { supportedCurrencies.getValue(it) }
                    .map { it.toOneUnitInputQuery(config.currencies.internalPrecision, supportedCurrencies.keys.toList()) }
                    .map { exchangeInputQuery(it) }
                    .toList().toTypedArray()
        }
    }

    fun exchangeInputQuery(inputQuery: InputQuery): ExchangeResults {
        val baseValue = inputQuery.expressionResult.toApiBaseValue(inputQuery.baseCurrency)
        return ExchangeResults(
                input = inputQuery,
                rates = inputQuery.targets.map { ExchangedSum(
                        currency = supportedCurrencies.getValue(it),
                        sum = baseValue / exchangeRates.getValue(it)
                ) }
        )
    }

    fun exchangeToApiBase(value: BigDecimal, code: String): BigDecimal {
        return value.toApiBaseValue(code)
    }

    fun ratesDashboard(): Array<ExchangeResults> {
        return exchangeRatesDashboard
    }

    private fun BigDecimal.toApiBaseValue(sourceBase: String) =
            if (sourceBase == config.currencies.apiBase) {
                this.toConfScale(config)
            } else {
                val rate = exchangeRates[sourceBase]
                        ?: throw IllegalArgumentException("Unknown currency provided ($sourceBase)")
                this * rate
            }

}
