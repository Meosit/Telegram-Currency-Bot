package by.mksn.gae.easycurrbot

import by.mksn.gae.easycurrbot.exchange.Currency
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
            val outputWidthChars: Int,
            val maxMessageLength: Int,
            val creatorUsername: String,
            val creatorId: String
    )

    data class Routes(
            val exchange: String,
            val updates: String,
            val register: String,
            val unregister: String
    )

    data class Strings(
            val kiloSpecialChar: String,
            val telegram: Telegram,
            val tokenNames: TokenNames,
            val errors: Errors
    ) {
        data class Telegram(
                val help: String,
                val patterns: String,
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
                val kilo: String,
                val mega: String,
                val leftPar: String,
                val rightPar: String,
                val multiply: String,
                val divide: String,
                val minus: String,
                val plus: String,
                val whitespace: String,
                val currency: String,
                val exclamation: String,
                val ampersand: String,
                val nativeConversionUnion: String
        )

        data class Errors(
                val invalidCurrencyAlias: String,
                val illegalCurrencyPlacement: String,
                val unparsedReminder: String,
                val mismatchedToken: String,
                val noMatchingToken: String,
                val unexpectedEOF: String,
                val divisionByZero: String,
                val unexpectedError: String,
                val queryTooBig: String
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