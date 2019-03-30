package by.mksn.gae.easycurrbot.service

import by.mksn.gae.easycurrbot.AppConfig
import by.mksn.gae.easycurrbot.entity.*
import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.url
import me.ivmg.telegram.entities.Message
import me.ivmg.telegram.network.Response
import java.text.DecimalFormat

class OutputMessageService(private val httpClient: HttpClient, private val config: AppConfig) {

    private val sumFormat = DecimalFormat(config.currencies.outputSumPattern)
    private val justCalculateFormat = DecimalFormat("#0.####")
    private val gson = Gson()

    private fun generateOutputMarkdown(results: ExchangeResults): String {
        val exprPrefix = when (results.input.type) {
            ExpressionType.SINGLE_VALUE -> if (!results.input.isOneUnit()) "" else
                "\uD83D\uDCB9 _${config.strings.telegram.inlineTitles.dashboard.format(results.input.involvedCurrencies.first())}_\n"
            else -> "#️⃣ _${results.input.expression}=_\n"
        }
        return exprPrefix + results.rates.joinToString(separator = "\n") { "${it.currency.symbol} `${sumFormat.format(it.sum)}`" }
    }

    suspend fun sendMarkdownToChat(chatId: String, text: String, replyMessageId: String? = null) {
        httpClient.post<Response<Message>> {
            url("${config.telegram.apiUrl}/sendMessage")
            parameter("text", text)
            parameter("parse_mode", "Markdown")
            parameter("chat_id", chatId)
            replyMessageId?.let { parameter("reply_to_message_id", it) }
        }
    }

    suspend fun sendResultToChat(chatId: String, results: ExchangeResults) {
        val markdown = generateOutputMarkdown(results)
        httpClient.post<String> {
            url("${config.telegram.apiUrl}/sendMessage")
            parameter("text", markdown)
            parameter("parse_mode", "Markdown")
            parameter("chat_id", chatId)
        }
    }

    suspend fun sendResultToInlineQuery(queryId: String, vararg resultsList: ExchangeResults) {
        val inlineResults = resultsList.map { results ->
            val markdown = generateOutputMarkdown(results)
            val queryDescription = markdown.replace("`", "")
                    .replace("_", "")
                    .replace("\n", " ")

            val title = when {
                results.input.isOneUnit() -> config.strings.telegram.inlineTitles.dashboard.format(results.input.involvedCurrencies.firstOrNull() ?: "???")
                else -> config.strings.telegram.inlineTitles.exchange.format(
                        if(results.input.type != ExpressionType.MULTI_CURRENCY_EXPR) sumFormat.format(results.input.expressionResult) else "\uD83C\uDF10",
                        results.input.involvedCurrencies.joinToString(","),
                        (results.input.targets - results.input.involvedCurrencies).joinToString(",")
                )
            }
            val thumbUrl = if (results.input.isOneUnit())
                "${config.serverUrl}/thumbs/dashboard.png" else "${config.serverUrl}/thumbs/exchange.png"

            InlineQueryResultArticle(
                    title = title,
                    description = queryDescription,
                    inputMessageContent = InputTextMessageContent(markdown, "Markdown"),
                    thumbUrl = thumbUrl
            )
        }


        val resultsJson = if (resultsList.size == 1 && resultsList.single().input.type == ExpressionType.SINGLE_CURRENCY_EXPR) {
            val results = resultsList.single()
            gson.toJson(inlineResults + InlineQueryResultArticle(
                    title = config.strings.telegram.inlineTitles.calculate,
                    description = "${results.input.expression} = ${sumFormat.format(results.input.expressionResult)}",
                    inputMessageContent = InputTextMessageContent(
                            "`${results.input.expression} = ${justCalculateFormat.format(results.input.expressionResult)}`",
                            "Markdown"
                    ),
                    thumbUrl = "${config.serverUrl}/thumbs/calculate.png"
            ))
        } else {
            gson.toJson(inlineResults)
        }

        httpClient.post<Response<Boolean>> {
            url("${config.telegram.apiUrl}/answerInlineQuery")
            parameter("inline_query_id", queryId)
            parameter("results", resultsJson)
        }
    }

    suspend fun sendErrorToInlineQuery(queryId: String, error: InputError) {
        val resultsJson = gson.toJson(listOf(InlineQueryResultArticle(
                title = error.message,
                description = error.toSingleLine(),
                inputMessageContent = InputTextMessageContent(error.toMarkdown(), "Markdown"),
                thumbUrl = "${config.serverUrl}/thumbs/error.png"
        )))

        httpClient.post<Response<Boolean>> {
            url("${config.telegram.apiUrl}/answerInlineQuery")
            parameter("inline_query_id", queryId)
            parameter("results", resultsJson)
        }
    }


}
