package by.mksn.gae.easycurrbot.service

import by.mksn.gae.easycurrbot.AppConfig
import by.mksn.gae.easycurrbot.entity.ExchangeResults
import by.mksn.gae.easycurrbot.entity.InlineQueryResultArticle
import by.mksn.gae.easycurrbot.entity.InputTextMessageContent
import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.url
import me.ivmg.telegram.entities.Message
import me.ivmg.telegram.network.Response
import java.text.DecimalFormat

class OutputMessageService(private val httpClient: HttpClient, private val config: AppConfig) {

    private val decimalFormat = DecimalFormat(config.currencies.outputSumPattern)
    private val gson = Gson()

    private fun generateOutputMarkdown(results: ExchangeResults): String {
        val exprPrefix = when {
            results.input.sumExpression.toBigDecimalOrNull() == null ->
                "#️⃣ _${results.input.sumExpression}=_\n"
            results.input.sumExpression == "1" ->
                "\uD83D\uDCB9 _${config.messages.telegram.inlineTitles.dashboard.format(results.input.base)}_\n"
            else -> ""
        }
        return exprPrefix + results.rates.joinToString(separator = "\n") { "${it.currency.symbol} `${decimalFormat.format(it.sum)}`" }
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

    suspend fun sendResultToChat(chatId: String, results: ExchangeResults, replyMessageId: String? = null) {
        val markdown = generateOutputMarkdown(results)
        httpClient.post<String> {
            url("${config.telegram.apiUrl}/sendMessage")
            parameter("text", markdown)
            parameter("parse_mode", "Markdown")
            parameter("chat_id", chatId)
            replyMessageId?.let { parameter("reply_to_message_id", it) }
        }
    }

    suspend fun sendResultToInlineQuery(queryId: String, vararg resultsList: ExchangeResults) {
        val inlineResults = resultsList.map { results ->
            val markdown = generateOutputMarkdown(results)
            val queryDescription = markdown.replace("`", "")
                    .replace("_", "")
                    .replace("\n", " ")
            val title = if (results.input.sumExpression == "1") {
                config.messages.telegram.inlineTitles.dashboard.format(results.input.base)
            } else with(results.rates.first()) {
                config.messages.telegram.inlineTitles.exchange.format(
                        decimalFormat.format(sum),
                        currency.code,
                        results.input.targets.drop(1).joinToString(", ") { it }
                )
            }

            val thumbUrl = if (results.input.sumExpression == "1")
                "${config.serverUrl}/thumbs/dashboard.png" else "${config.serverUrl}/thumbs/exchange.png"
            InlineQueryResultArticle(
                    title = title,
                    description = queryDescription,
                    inputMessageContent = InputTextMessageContent(markdown, "Markdown"),
                    thumbUrl = thumbUrl
            )
        }


        val resultsJson = if (resultsList.size == 1 && resultsList.single().input.sumExpression.toBigDecimalOrNull() == null) {
            val results = resultsList.single()
            gson.toJson(inlineResults + InlineQueryResultArticle(
                    title = config.messages.telegram.inlineTitles.calculate,
                    description = "${results.input.sumExpression} = ${decimalFormat.format(results.input.sum)}",
                    inputMessageContent = InputTextMessageContent(
                            "`${results.input.sumExpression} = ${decimalFormat.format(results.input.sum)}`",
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


}
