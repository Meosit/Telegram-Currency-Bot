package by.mksn.gae.easycurrbot.route

import by.mksn.gae.easycurrbot.AppConfig
import by.mksn.gae.easycurrbot.entity.Update
import by.mksn.gae.easycurrbot.entity.happenedAfter
import by.mksn.gae.easycurrbot.service.*
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import io.ktor.util.date.GMTDate
import me.ivmg.telegram.entities.InlineQuery
import me.ivmg.telegram.entities.Message
import org.slf4j.LoggerFactory


private val LOG = LoggerFactory.getLogger(Application::class.java)!!

fun Routing.handleUpdate(config: AppConfig, service: CombinedService, serverStartTime: GMTDate) {
    post(config.routes.updates) {
        val update = call.receive<Update>()
        try {
            service.exchange.invalidateExchangeRates()
            when {
                update.message?.text != null -> {
                    if (update.message.happenedAfter(serverStartTime)) {
                        update.message.handle(config, service)
                    }
                }
                update.inlineQuery != null -> {
                    update.inlineQuery.handle(service)
                }
            }
        } catch (e: Exception) {
            LOG.info("Unexpected error", e)
        }
        call.respond(HttpStatusCode.OK)
    }
}

private suspend fun InlineQuery.handle(service: CombinedService) {
    val user = with(from) { username ?: "$firstName $lastName" }
    when {
        query.isBlank() -> {
            LOG.info("[InlineQuery] User: $user\n Empty query dashboard")
            val exchangeResultsList = service.exchange.ratesDashboard()
            service.output.sendResultToInlineQuery(id, *exchangeResultsList)
        }
        else -> {
            val parsedQuery = service.input.parse(query)
            LOG.info("[InlineQuery] User: $user\nInput: '$query'\nParsed: $parsedQuery")
            parsedQuery.fold(success = {
                val exchangeResults = service.exchange.exchangeInputQuery(it)
                service.output.sendResultToInlineQuery(id, exchangeResults)
            }, failure = {
                service.output.sendErrorToInlineQuery(id, it)
            })
        }
    }
}

private suspend fun Message.handle(config: AppConfig, service: CombinedService) {
    when (text) {
        "/start" -> service.output
                .sendMarkdownToChat(chat.id.toString(), config.strings.telegram.start)
        "/help" -> service.output
                .sendMarkdownToChat(chat.id.toString(), config.strings.telegram.help)
        "", null -> Unit
        else -> {
            val user = with(chat) { username ?: "$firstName $lastName" }
            val query = service.input.parse(text!!)
            LOG.info("[Message] User: $user\nInput: '$text'\nParsed: $query")
            query.fold(success = {
                val exchangeResults = service.exchange.exchangeInputQuery(it)
                service.output.sendResultToChat(chat.id.toString(), exchangeResults)
            }, failure = {
                service.output.sendMarkdownToChat(chat.id.toString(), it.toMarkdown(), replyMessageId = messageId.toString())
            })
        }
    }
}
