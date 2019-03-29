package by.mksn.gae.easycurrbot

import by.mksn.gae.easycurrbot.entity.Currency
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract

data class AppConfig(
        val serverUrl: String,
        val currencies: Currencies,
        val telegram: Telegram,
        val routes: Routes,
        val strings: Strings
) {
    data class Currencies(
            val apiUrl: String,
            val internalPrecision: Int,
            val outputSumPattern: String,
            val apiBase: String,
            val default: List<String>,
            val dashboard: List<String>,
            val supported: List<Currency>
    )

    data class Telegram(
            val token: String,
            val apiUrl: String,
            val outputWidthChars: Int
    )

    data class Routes(
            val exchange: String,
            val updates: String,
            val register: String,
            val unregister: String
    )

    data class Strings(
            val telegram: Telegram,
            val tokenNames: TokenNames,
            val errors: Errors
    ) {
        data class Telegram(
                val start: String,
                val help: String,
                val inlineTitles: InlineTitles
        ) {
            data class InlineTitles(
                    val exchange: String,
                    val calculate: String,
                    val dashboard: String
            )
        }

        data class TokenNames(
                val number: String,
                val leftPar: String,
                val rightPar: String,
                val multiply: String,
                val divide: String,
                val minus: String,
                val plus: String,
                val whitespace: String,
                val currency: String
        )

        data class Errors(
                val invalidMatcherProvided: String,
                val illegalCurrencyPlacement: String,
                val unparsedReminder: String,
                val mismatchedToken: String,
                val noMatchingToken: String,
                val unexpectedEOF: String,
                val divisionByZero: String,
                val unexpectedError: String
        )
    }

    companion object {
        private const val CONFIG_PATH = "app"

        fun create(resourceBasename: String): AppConfig {
            val appConfiguration = ConfigFactory.load(resourceBasename).resolve().getConfig(CONFIG_PATH)

            return appConfiguration.extract()
        }
    }
}