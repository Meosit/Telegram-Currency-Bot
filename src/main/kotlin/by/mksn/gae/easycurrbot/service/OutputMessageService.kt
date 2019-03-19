package by.mksn.gae.easycurrbot.service

import by.mksn.gae.easycurrbot.config.CurrenciesConfig
import by.mksn.gae.easycurrbot.config.TelegramConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import io.ktor.client.HttpClient
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.url
import me.ivmg.telegram.entities.InlineQuery
import me.ivmg.telegram.entities.Message
import me.ivmg.telegram.network.Response
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.text.DecimalFormat
import java.util.*

/**
 * @author Mikhail Snitavets
 */
class OutputMessageService(private val httpClient: HttpClient, currConf: CurrenciesConfig, tgConf: TelegramConfig) {

    companion object {
        val LOG = LoggerFactory.getLogger(OutputMessageService::class.java)!!
    }


    private val apiBase = tgConf.apiUrl
    private val currencies = currConf.currencies.supported.associateBy { it.code }
    private val decimalFormat = DecimalFormat("#0.##")
    private val gson = Gson()

    private fun generateOutputMarkdown(results: Map<String, BigDecimal>) =
            results.mapKeys { currencies.getValue(it.key) }.entries
                    .joinToString("\n") { "${it.key.symbol} `${decimalFormat.format(it.value)}`" }

    suspend fun sendResultMessage(message: Message, results: Map<String, BigDecimal>) {
        val markdown = generateOutputMarkdown(results)
        httpClient.post<Response<Message>> {
            url("$apiBase/sendMessage")
            parameter("text", markdown)
            parameter("parse_mode", "Markdown")
            parameter("chat_id", message.chat.id.toString())
            parameter("reply_to_message_id", message.messageId.toString())
        }
    }

    suspend fun sendResultMarkdown(message: Message, text: String) {
        httpClient.post<Response<Message>> {
            url("$apiBase/sendMessage")
            parameter("text", text)
            parameter("parse_mode", "Markdown")
            parameter("chat_id", message.chat.id.toString())
            parameter("reply_to_message_id", message.messageId.toString())
        }
    }

    suspend fun sendResultQuery(query: InlineQuery, results: Map<String, BigDecimal>) {
        val markdown = generateOutputMarkdown(results)
        val json = gson.toJson(listOf(InlineQueryResultArticle(
                title = with(results.entries.first()) { "${decimalFormat.format(value)} $key ⇒ ${results.entries.drop(1).joinToString(", ") { it.key }}" },
                description = markdown.replace("`", "").replace("\n", " "),
                inputMessageContent = InputTextMessageContent(markdown, "Markdown")
        )))
        LOG.info(json)
        val res = httpClient.post<String> {
            url("$apiBase/answerInlineQuery")
            parameter("inline_query_id", query.id)
            parameter("results", json)
        }
        LOG.info(res)
    }

    data class InlineQueryResultArticle(
            val type: String = "article",
            val id: String = UUID.randomUUID().toString(),
            val title: String,
            val description: String,
            @SerializedName("input_message_content") val inputMessageContent: InputTextMessageContent
    )

    data class InputTextMessageContent(
            @SerializedName("message_text") val messageText: String,
            @SerializedName("parse_mode") val parseMode: String
    )
}