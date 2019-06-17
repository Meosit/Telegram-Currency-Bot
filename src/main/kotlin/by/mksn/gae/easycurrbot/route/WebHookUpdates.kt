package by.mksn.gae.easycurrbot.route

import by.mksn.gae.easycurrbot.AppConfig
import by.mksn.gae.easycurrbot.entity.Update
import by.mksn.gae.easycurrbot.entity.happenedAfter
import by.mksn.gae.easycurrbot.service.CombinedService
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.client.features.BadResponseStatusException
import io.ktor.client.response.readText
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.date.GMTDate
import me.ivmg.telegram.entities.Chat
import me.ivmg.telegram.entities.InlineQuery
import me.ivmg.telegram.entities.Message
import me.ivmg.telegram.entities.User
import org.slf4j.LoggerFactory


private val LOG = LoggerFactory.getLogger(Application::class.java)!!

@KtorExperimentalAPI
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
            val cause = (e as? BadResponseStatusException)?.response?.readText() ?: e.message
            LOG.info("Unexpected error", e)
            LOG.info("Update: $update")
            LOG.info("Cause: $cause")
            service.output.sendMarkdownToChat(config.telegram.creatorId, "Error received:\n" +
                    "```\nQuery: ${update.message?.text ?: update.editedMessage?.text ?: update.inlineQuery?.query}\n\nCause: $cause```")
        }
        call.respond(HttpStatusCode.OK)
    }
}

private suspend fun InlineQuery.handle(config: AppConfig, service: CombinedService) {
    val user = from.userReadableName(config)
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
        "/start", "/help" -> service.output
                .sendMarkdownToChat(chat.id.toString(), config.strings.telegram.help)
        "/patterns" -> service.output
                .sendMarkdownToChat(chat.id.toString(), config.strings.telegram.patterns)
        "", null -> Unit
        else -> {
            val user = chat.userReadableName(config)
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

private fun Chat.userReadableName(config: AppConfig) = with(this) {
    (username ?: "$firstName ${lastName ?: ""} ($id)") +
            if (username == config.telegram.creatorUsername) "" else " (not Creator)"
}

private fun User.userReadableName(config: AppConfig) = with(this) {
    (username ?: "$firstName ${lastName ?: ""} ($id)") +
            if (username == config.telegram.creatorUsername) "" else " (not Creator)"
}