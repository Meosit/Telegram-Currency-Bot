package by.mksn.gae.easycurrbot.entity

import com.google.gson.annotations.SerializedName
import io.ktor.util.date.GMTDate
import me.ivmg.telegram.entities.*
import me.ivmg.telegram.entities.Update
import me.ivmg.telegram.entities.payment.PreCheckoutQuery
import me.ivmg.telegram.entities.payment.ShippingQuery
import me.ivmg.telegram.types.DispatchableObject
import java.util.*

/**
 * @author Mikhail Snitavets
 * New version of [Update] with additional fields.
 */
data class Update constructor(
        @SerializedName("update_id") val updateId: Long,
        val message: Message?,
        @SerializedName("edited_message") val editedMessage: Message?,
        @SerializedName("channel_post") val channelPost: Message?,
        @SerializedName("edited_channel_post") val editedChannelPost: Message?,
        @SerializedName("inline_query") val inlineQuery: InlineQuery?,
        @SerializedName("chosen_inline_result") val chosenInlineResult: ChosenInlineResult?,
        @SerializedName("callback_query") val callbackQuery: CallbackQuery?,
        @SerializedName("pre_checkout_query") val preCheckoutQuery: PreCheckoutQuery?,
        @SerializedName("shipping_query") val shippingQuery: ShippingQuery?
) : DispatchableObject

fun Message.happenedAfter(other: GMTDate) = GMTDate(date.toLong() * 1000) >= other


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