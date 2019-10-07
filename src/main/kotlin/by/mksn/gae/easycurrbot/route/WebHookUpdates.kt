package by.mksn.gae.easycurrbot.route

import by.mksn.gae.easycurrbot.AppConfig
import by.mksn.gae.easycurrbot.entity.Update
import by.mksn.gae.easycurrbot.entity.happenedAfter
import by.mksn.gae.easycurrbot.service.CombinedService
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.client.features.ResponseException
import io.ktor.client.response.readText
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import io.ktor.util.date.GMTDate
import me.ivmg.telegram.entities.Chat
import me.ivmg.telegram.entities.InlineQuery
import me.ivmg.telegram.entities.Message
import me.ivmg.telegram.entities.User
import java.util.logging.Level
import java.util.logging.Logger


private val LOG = Logger.getLogger(Application::class.simpleName)!!

fun Routing.handleUpdate(config: AppConfig, service: CombinedService, serverStartTime: GMTDate) {
    post(config.routes.updates) {
        val update = call.receive<Update>()
        try {
            service.exchange.invalidateExchangeRates()
            with(update) {
                when {
                    editedMessage?.text != null -> {
                        if (editedMessage.happenedAfter(serverStartTime)) {
                            editedMessage.handle(config, service)
                        }
                    }
                    message?.text != null -> {
                        if (message.happenedAfter(serverStartTime)) {
                            message.handle(config, service)
                        }
                    }
                    inlineQuery != null -> {
                        inlineQuery.handle(config, service)
                    }
                }
            }
        } catch (e: Exception) {
            val cause = (e as? ResponseException)?.response?.readText() ?: e.message ?: "No exception message supplied"
            val queryString = (update.message ?: update.editedMessage)?.text ?: update.inlineQuery?.query
            val user = (update.message ?: update.editedMessage)?.chat?.userReadableName(config) ?: update.inlineQuery?.from?.userReadableName(config)
            if ("query is too old" in cause) {
                LOG.warning("\nIgnored 'query is too old'-like error")
            } else {
                LOG.log(Level.SEVERE, "Unexpected error: \nCause: $cause\nUpdate: $update", e)
                service.output.sendMarkdownToChat(config.telegram.creatorId, "Error received:\n```\nQuery: $queryString\nUser: $user\n\nCause: $cause```")
            }
        }
        call.respond(HttpStatusCode.OK)
    }
}

private suspend fun InlineQuery.handle(config: AppConfig, service: CombinedService) {
    val user = from.userReadableName(config)
    when {
        query.isBlank() -> {
            LOG.info("[InlineQuery]\nUser: $user\n Empty query dashboard")
            val exchangeResultsList = service.exchange.ratesDashboard()
            service.output.sendResultToInlineQuery(id, *exchangeResultsList)
        }
        else -> {
            val parsedQuery = service.input.parse(query)
            LOG.info("[InlineQuery]\nUser: $user\nInput: '$query'\nParsed: $parsedQuery")
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
        "/start", "/help" -> service.output
                .sendMarkdownToChat(chat.id.toString(), config.strings.telegram.help)
        "/patterns" -> service.output
                .sendMarkdownToChat(chat.id.toString(), config.strings.telegram.patterns)
        "", null -> Unit
        else -> {
            val user = chat.userReadableName(config)
            val query = service.input.parse(text!!)
            LOG.info("[Message]\nUser: $user\nInput: '$text'\nParsed: $query")
            query.fold(success = {
                val exchangeResults = service.exchange.exchangeInputQuery(it)
                service.output.sendResultToChat(chat.id.toString(), exchangeResults)
            }, failure = {
                service.output.sendMarkdownToChat(chat.id.toString(), it.toMarkdown(), replyMessageId = messageId.toString())
            })
        }
    }
}

private fun Chat.userReadableName(config: AppConfig) = with(this) {
    (username ?: "$firstName ${lastName ?: ""} ($id)") +
            if (username == config.telegram.creatorUsername) "" else " (not Creator)"
}

private fun User.userReadableName(config: AppConfig) = with(this) {
    (username ?: "$firstName ${lastName ?: ""} ($id)") +
            if (username == config.telegram.creatorUsername) "" else " (not Creator)"
}