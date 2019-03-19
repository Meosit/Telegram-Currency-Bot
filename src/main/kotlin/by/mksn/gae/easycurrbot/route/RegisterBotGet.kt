package by.mksn.gae.easycurrbot.route

import by.mksn.gae.easycurrbot.entity.isUpdateType
import by.mksn.gae.easycurrbot.config.TelegramConfig
import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.html.respondHtml
import io.ktor.routing.Route
import io.ktor.routing.get
import kotlinx.html.*
import me.ivmg.telegram.network.Response

fun Route.registerBotGet(tgConf: TelegramConfig, httpClient: HttpClient) =
        get("/bot/${tgConf.token}/register") {
            val updateTypes = call.request.queryParameters.getAll("updates")
                    ?.filter { it.isUpdateType() }
                    ?.joinToString(prefix = "[", postfix = "]", separator = ",")
                    ?: "[]"
            val (_, success, errCode, errDescription) = httpClient.post<Response<Any>> {
                url("${tgConf.apiUrl}${tgConf.token}/setWebhook")
                parameter("url", "${tgConf.botUrl}/bot/${tgConf.token}/webhook")
                parameter("allowed_updates", updateTypes)
            }
            call.respondHtml {
                head { title { +"Webhook Control" } }
                body {
                    if (success) {
                        h3 { +"Success" }
                        p { +"Webhook was successfully set to the following update types: $updateTypes." }
                    } else {
                        h3 { +"Failed" }
                        p { +"Code: $errCode" }
                        p { +"Description: '$errDescription'" }
                    }
                }
            }
        }

fun Route.unregisterBotGet(tgConf: TelegramConfig, httpClient: HttpClient) =
        get("/bot/${tgConf.token}/unregiser") {
            val (_, success, errCode, errDescription) =
                    httpClient.post<Response<Any>>("${tgConf.apiUrl}/deleteWebhook")
            call.respondHtml {
                head { title { +"Webhook Control" } }
                body {
                    if (success) {
                        h3 { +"Success" }
                        p { +"Webhook was successfully unset." }
                    } else {
                        h3 { +"Failed" }
                        p { +"Code: $errCode" }
                        p { +"Description: '$errDescription'" }
                    }
                }
            }
        }