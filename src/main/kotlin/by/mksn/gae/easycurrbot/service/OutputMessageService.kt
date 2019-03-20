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
import org.slf4j.LoggerFactory
import java.text.DecimalFormat

class OutputMessageService(private val httpClient: HttpClient, private val config: AppConfig) {

    private val decimalFormat = DecimalFormat("#0.##")
    private val gson = Gson()

    private fun generateOutputMarkdown(results: ExchangeResults): String {
        val exprPrefix = if (results.input.sumExpression.toBigDecimalOrNull() == null)
            "#️⃣ _${results.input.sumExpression}=_\n" else ""
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

    suspend fun sendResultToInlineQuery(queryId: String, results: ExchangeResults) {
        val markdown = generateOutputMarkdown(results)
        val queryDescription = markdown
                .replace("`", "")
                .replace("_", "")
                .replace("\n", " ")

        val title = with(results.rates.first()) {
            val convertedCurrencies = results.input.targets.drop(1).joinToString(", ") { it }
            "${decimalFormat.format(sum)} ${currency.code} ⇒ $convertedCurrencies"
        }

        val json = gson.toJson(listOf(InlineQueryResultArticle(
                title = title,
                description = queryDescription,
                inputMessageContent = InputTextMessageContent(markdown, "Markdown")
        )))


        httpClient.post<Response<Boolean>> {
            url("${config.telegram.apiUrl}/answerInlineQuery")
            parameter("inline_query_id", queryId)
            parameter("results", json)
        }
    }


}
