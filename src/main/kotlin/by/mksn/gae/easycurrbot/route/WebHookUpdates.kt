package by.mksn.gae.easycurrbot.route

import by.mksn.gae.easycurrbot.AppConfig
import by.mksn.gae.easycurrbot.entity.Update
import by.mksn.gae.easycurrbot.entity.happenedAfter
import by.mksn.gae.easycurrbot.entity.success
import by.mksn.gae.easycurrbot.service.*
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.client.HttpClient
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

fun Routing.handleUpdate(config: AppConfig, httpClient: HttpClient, serverStartTime: GMTDate) {

    val inputService = InputQueryService(config)
    val exchangeService = ExchangeRateService(httpClient, config)
    val outputService = OutputMessageService(httpClient, config)

    post(config.routes.updates) {
        val update = call.receive<Update>()
        try {
            when {
                update.message?.text != null -> {
                    if (update.message.happenedAfter(serverStartTime)) {
                        update.message.handle(config, inputService, exchangeService, outputService)
                    }
                }
                update.inlineQuery != null -> { update.inlineQuery.handle(inputService, exchangeService, outputService) }
            }
        } catch (e: Exception) {
            LOG.info("Unexpected error", e)
        }
        call.respond(HttpStatusCode.OK)
    }
}

private suspend fun InlineQuery.handle(inputService: InputQueryService,
                                       exchangeService: ExchangeRateService,
                                       outputService: OutputMessageService) {
    val user = with(from) { username ?: "$firstName $lastName" }
    when {
        query.isBlank() -> {
            LOG.info("[InlineQuery] User: $user\n Empty query dashboard")
            val exchangeResultsList = exchangeService.ratesDashboard()
            outputService.sendResultToInlineQuery(id, *exchangeResultsList)
        }
        else -> {
            val parsedQuery = inputService.parse(query)
            LOG.info("[InlineQuery] User: $user\nInput: '$query'\nParsed: $parsedQuery")
            parsedQuery.success {
                val exchangeResults = exchangeService.exchange(it)
                outputService.sendResultToInlineQuery(id, exchangeResults)
            }
        }
    }
    if (query.isBlank()) {
        LOG.info("Ignored empty query")
        return
    } else {

    }
}

private suspend fun Message.handle(config: AppConfig,
                                   inputService: InputQueryService,
                                   exchangeService: ExchangeRateService,
                                   outputService: OutputMessageService) {
    when (text) {
        "/start" -> outputService
                .sendMarkdownToChat(chat.id.toString(), config.messages.telegram.start)
        "/help" -> outputService
                .sendMarkdownToChat(chat.id.toString(), config.messages.telegram.help)
        "", null -> Unit
        else -> {
            val user = with(chat) { username ?: "$firstName $lastName" }
            val query = inputService.parse(text!!)
            LOG.info("[Message] User: $user\nInput: '$text'\nParsed: $query")
            query.fold(success = {
                val exchangeResults = exchangeService.exchange(it)
                outputService.sendResultToChat(chat.id.toString(), exchangeResults, replyMessageId = messageId.toString())
            }, failure = {
                outputService.sendMarkdownToChat(chat.id.toString(), it, replyMessageId = messageId.toString())
            })
        }
    }
}
