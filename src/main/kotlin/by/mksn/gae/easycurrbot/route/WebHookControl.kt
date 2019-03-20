package by.mksn.gae.easycurrbot.route

import by.mksn.gae.easycurrbot.AppConfig
import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.html.respondHtml
import io.ktor.routing.Routing
import io.ktor.routing.get
import kotlinx.html.*
import me.ivmg.telegram.network.Response


fun Routing.registerBotGet(config: AppConfig, httpClient: HttpClient) {
    get(config.routes.register) {
        val updateTypes = call.request.queryParameters.getAll("updates")
                ?.joinToString(prefix = "[", postfix = "]", separator = ",")
                ?: "[]"
        val (_, success, errCode, errDescription) = httpClient.post<Response<Any>> {
            url("${config.telegram.apiUrl}/setWebhook")
            parameter("url", "${config.serverUrl}${config.routes.updates}")
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
}

fun Routing.unregisterBotGet(config: AppConfig, httpClient: HttpClient) {
    get(config.routes.unregister) {
        val (_, success, errCode, errDescription) =
                httpClient.post<Response<Any>>("${config.telegram.apiUrl}/deleteWebhook")
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
}