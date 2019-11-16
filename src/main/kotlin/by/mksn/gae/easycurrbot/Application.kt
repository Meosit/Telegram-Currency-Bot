package by.mksn.gae.easycurrbot

import by.mksn.gae.easycurrbot.exchange.ExchangeRateService
import by.mksn.gae.easycurrbot.input.InputQueryParser
import by.mksn.gae.easycurrbot.output.OutputMessageService
import by.mksn.gae.easycurrbot.route.*
import by.mksn.gae.easycurrbot.util.CombinedService
import by.mksn.gae.easycurrbot.util.network.UrlFetch
import com.google.gson.FieldNamingPolicy
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.gson.gson
import io.ktor.routing.routing
import io.ktor.util.date.GMTDate
import io.ktor.util.date.minus

fun Application.main() {
    install(DefaultHeaders)
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
    val serverStartTime = GMTDate() - 60000 // to handle first request after long startup

    val exchange = ExchangeRateService(httpClient, config)
    val service = CombinedService(
            exchange = exchange,
            input = InputQueryParser(config, exchange),
            output = OutputMessageService(httpClient, config)
    )

    routing {
        rootPage(config)
        exchangeEndpoint(config, service)
        registerBotGet(config, httpClient)
        unregisterBotGet(config, httpClient)
        handleUpdate(config, service, serverStartTime)
    }

}
