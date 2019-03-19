package by.mksn.gae.easycurrbot.config

import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.ktor.config.HoconApplicationConfig
import io.ktor.util.KtorExperimentalAPI

/**
 * @author Mikhail Snitavets
 */
data class TelegramConfig(
        val token: String,
        val botUrl: String,
        val apiUrl: String,
        val helpMessage: String
) {

    companion object {
        fun create(resourceBasename: String): TelegramConfig {
            val telegramConfiguration = ConfigFactory.load(resourceBasename).resolve()
            return telegramConfiguration.extract()
        }
    }
}