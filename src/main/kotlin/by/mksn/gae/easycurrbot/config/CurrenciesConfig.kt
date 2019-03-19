package by.mksn.gae.easycurrbot.config

import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.ktor.util.KtorExperimentalAPI



data class Currency(val code: String, val symbol: String, val matchPatterns: List<String>)

class CurrenciesConfig constructor(
        val apiUrl: String,
        val currencies: Currencies
) {
    class Currencies constructor(
            val base: String,
            val default: List<String>,
            val supported: List<Currency>
    )

    companion object {
        fun create(resourceBasename: String): CurrenciesConfig {
            val currenciesConfiguration = ConfigFactory.load(resourceBasename).resolve()
            return currenciesConfiguration.extract()
        }
    }

}