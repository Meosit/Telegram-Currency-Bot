package by.mksn.gae.easycurrbot

import by.mksn.gae.easycurrbot.network.UrlFetch
import by.mksn.gae.easycurrbot.route.handleUpdate
import by.mksn.gae.easycurrbot.route.registerBotGet
import by.mksn.gae.easycurrbot.route.rootGet
import by.mksn.gae.easycurrbot.route.unregisterBotGet
import by.mksn.gae.easycurrbot.service.ExchangeRateService
import com.google.gson.FieldNamingPolicy
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.client.HttpClient
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.gson.gson
import io.ktor.routing.routing
import io.ktor.util.date.GMTDate

fun Application.main() {
    install(DefaultHeaders)
    install(CallLogging)
    install(ContentNegotiation) {
        gson {
            setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        }
    }
    val httpClient = HttpClient(UrlFetch) {
        install(JsonFeature) {
            serializer = GsonSerializer {
                setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            }
        }
    }
    val config: AppConfig = AppConfig.create("application.conf")
    val serverStartTime = GMTDate()

    routing {
        rootGet()
        registerBotGet(config, httpClient)
        unregisterBotGet(config, httpClient)
        handleUpdate(config, httpClient, serverStartTime)
    }

}
