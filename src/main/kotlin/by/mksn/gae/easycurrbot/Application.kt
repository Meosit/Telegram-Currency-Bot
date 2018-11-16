package by.mksn.gae.easycurrbot

import by.mksn.gae.easycurrbot.entity.Update
import by.mksn.gae.easycurrbot.entity.isUpdateType
import com.google.gson.FieldNamingPolicy
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.gson.gson
import io.ktor.html.respondHtml
import io.ktor.request.receive
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import kotlinx.html.*
import me.ivmg.telegram.entities.User
import me.ivmg.telegram.network.ApiClient
import me.ivmg.telegram.network.Response

const val API_TOKEN = "<TELEGRAM_API_TOKEN>"
const val API_URL = "https://api.telegram.org/bot"
const val SERVER_URL = "https://telegram-currency-bot.appspot.com"

fun Application.main() {
    install(DefaultHeaders)
    install(CallLogging)
    install(ContentNegotiation) {
        gson {
            setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        }
    }

    val httpClient = HttpClient() {
        install(JsonFeature) { serializer = GsonSerializer {
            setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        } }
    }
    val apiClient = ApiClient(API_TOKEN, API_URL)

    var isWebhookSet = false

    routing {
        get("/") {
            call.respondHtml {
                head {
                    title { +"Welcome Page" }
                }
                body {
                    p {
                        +"What are you looking here?"
                    }
                }
            }
        }
        get("/bot/$API_TOKEN/register") { _ ->
            if (!isWebhookSet) {
                val updateTypes = call.request.queryParameters.getAll("updates")
                        ?.filter { it.isUpdateType() }
                        ?.joinToString(prefix = "[", postfix = "]", separator = ",") ?: "[]"
                val (_, success, errCode, errDescription) = httpClient.post<Response<Any>> {
                    url("$API_URL$API_TOKEN/setWebhook")
                    parameter("url", "$SERVER_URL/bot/$API_TOKEN/webhook")
                    parameter("allowed_updates", updateTypes)

                }
                if (success) {
                    isWebhookSet = true
                    call.respondHtml {
                        head { title { +"Webhook Control" } }
                        body {
                            h3 { +"Success" }
                            p { +"Webhook was successfully set to the following update types: $updateTypes." }
                        }
                    }
                } else {
                    call.respondHtml {
                        head { title { +"Webhook Control" } }
                        body {
                            h3 { +"Failed" }
                            p { +"Code: $errCode" }
                            p { +"Description: '$errDescription'" }
                        }
                    }
                }
            } else {
                val (_, success, errCode, errDescription) = httpClient.post<Response<Any>>("$API_URL$API_TOKEN/setWebhook")
                if (success) {
                    isWebhookSet = false
                    call.respondHtml {
                        head { title { +"Webhook Control" } }
                        body {
                            h3 { +"Success" }
                            p { +"Webhook was successfully unset." }
                        }
                    }
                } else {
                    call.respondHtml {
                        head { title { +"Webhook Control" } }
                        body {
                            h3 { +"Failed" }
                            p { +"Code: $errCode" }
                            p { +"Description: '$errDescription'" }
                        }
                    }
                }
            }


        }
        get("/bot/me") { _ ->
            val (user, _, _, _) = httpClient.get<Response<User>>("$API_URL$API_TOKEN/getMe")
            if (user != null) {
                call.respondHtml {
                    head { title { +"About" } }
                    body {
                        h3 { +"About this bot" }
                        p { +"Id: ${user.id}" }
                        p { +"Is Bot: ${user.isBot}" }
                        p { +"First Name: ${user.firstName}" }
                        p { +"Last Name: ${user.lastName}" }
                        p { +"Username: ${user.username}" }
                        p { +"Language Code: ${user.languageCode}" }
                    }
                }
            }
        }
        post("/bot/$API_TOKEN/webhook") { _ ->
            val update = call.receive<Update>()
            update.message?.let {
                apiClient.sendMessage(
                        text = it.text ?: "Huh?",
                        chatId = it.chat.id,
                        parseMode = null,
                        disableWebPagePreview = null,
                        disableNotification = null,
                        replyToMessageId = it.messageId,
                        replyMarkup = null
                )
            }
        }
    }
}