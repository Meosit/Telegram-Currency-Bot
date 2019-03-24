package by.mksn.gae.easycurrbot

import by.mksn.gae.easycurrbot.entity.Currency
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract

data class AppConfig(
        val serverUrl: String,
        val currencies: Currencies,
        val telegram: Telegram,
        val routes: Routes,
        val messages: Messages
) {
    data class Currencies(
            val apiUrl: String,
            val internalPrecision: Int,
            val outputSumPattern: String,
            val base: String,
            val default: List<String>,
            val dashboard: List<String>,
            val supported: List<Currency>
    )

    data class Telegram(
            val token: String,
            val apiUrl: String
    )
    data class Routes(
            val updates: String,
            val register: String,
            val unregister: String
    )
    data class Messages(
            val telegram: Telegram,
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

        data class Errors(
                val invalidMatcherProvided: String,
                val invalidValueProvided: String,
                val illegalOperationResult: String,
                val emptyExpression: String,
                val invalidBinaryOperator: String,
                val invalidUnaryOperator: String,
                val invalidToken: String,
                val expectedEOF: String,
                val invalidRightOperand: String,
                val emptyLeftOperand: String,
                val unclosedParentheses: String
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