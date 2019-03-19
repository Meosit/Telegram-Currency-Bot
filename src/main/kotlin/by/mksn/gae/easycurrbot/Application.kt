package by.mksn.gae.easycurrbot

import by.mksn.gae.easycurrbot.config.CurrenciesConfig
import by.mksn.gae.easycurrbot.entity.Update
import by.mksn.gae.easycurrbot.network.UrlFetch
import by.mksn.gae.easycurrbot.route.registerBotGet
import by.mksn.gae.easycurrbot.route.rootGet
import by.mksn.gae.easycurrbot.route.unregisterBotGet
import by.mksn.gae.easycurrbot.config.TelegramConfig
import by.mksn.gae.easycurrbot.service.ExchangeRateService
import by.mksn.gae.easycurrbot.service.InputQueryService
import by.mksn.gae.easycurrbot.service.OutputMessageService
import com.google.gson.FieldNamingPolicy
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.client.HttpClient
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.gson.gson
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.date.GMTDate
import io.ktor.util.error


fun Application.main() {

    install(DefaultHeaders)
    install(CallLogging)
    install(ContentNegotiation) {
        gson {
            setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        }
    }
    val serverStartTime = GMTDate()

    val httpClient = HttpClient(UrlFetch) {
        expectSuccess = false
        install(JsonFeature) {
            serializer = GsonSerializer {
                setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            }
        }
    }
    val tgConf = TelegramConfig.create("telegram.conf")
    val currConf = CurrenciesConfig.create("currencies.conf")

    val inputQueryService = InputQueryService(currConf)
    val exchangeRateService = ExchangeRateService(httpClient, currConf)
    val outputMessageService = OutputMessageService(httpClient, currConf, tgConf)

    routing {
        rootGet()
        registerBotGet(tgConf, httpClient)
        unregisterBotGet(tgConf, httpClient)
        post("/bot/${tgConf.token}/webhook") {
            val update = call.receive<Update>()
            try {
                when {
                    update.message?.text != null -> {
                        with(update.message) {
                            if (GMTDate(date.toLong() * 1000) >= serverStartTime) {
                                when (text) {
                                    "/start" -> outputMessageService
                                            .sendResultMarkdown(update.message, "Привет! Спасибо за то что вы с нами!")
                                    "/help" -> outputMessageService
                                            .sendResultMarkdown(update.message, tgConf.helpMessage)
                                    else -> {
                                        val query = inputQueryService.parse(text!!)
                                        log.info(query.toString())
                                        if (query != null) {
                                            val exchange = exchangeRateService.exchange(query.value, query.base, query.targets)
                                            outputMessageService.sendResultMessage(update.message, exchange)
                                        } else {
                                            outputMessageService.sendResultMarkdown(update.message, "Неверный ввод.")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    update.inlineQuery != null -> {
                        val query = inputQueryService.parse(update.inlineQuery.query)
                        if (query != null) {
                            log.info(query.toString())
                            val exchange = exchangeRateService.exchange(query.value, query.base, query.targets)
                            outputMessageService.sendResultQuery(update.inlineQuery, exchange)
                        }
                    }
                }
            } catch (e: Exception) {
                log.error(e)
            }
            call.respond(HttpStatusCode.OK)
        }

    }

}
