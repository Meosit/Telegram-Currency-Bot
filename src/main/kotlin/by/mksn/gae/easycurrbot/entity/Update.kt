package by.mksn.gae.easycurrbot.entity

import com.google.gson.annotations.SerializedName
import me.ivmg.telegram.entities.CallbackQuery
import me.ivmg.telegram.entities.ChosenInlineResult
import me.ivmg.telegram.entities.InlineQuery
import me.ivmg.telegram.entities.Message
import me.ivmg.telegram.entities.Update
import me.ivmg.telegram.entities.payment.PreCheckoutQuery
import me.ivmg.telegram.entities.payment.ShippingQuery
import me.ivmg.telegram.types.DispatchableObject

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


/**
 * Generate list of key-value from start payload.
 * For more info {@link https://core.telegram.org/bots#deep-linking}
 */
fun Update.getStartPayload(delimiter: String = "-"): List<Pair<String, String>> {
    return message?.let {
        val parameters = it.text?.substringAfter("start ", "")
        if (parameters == null || parameters.isEmpty()) {
            return emptyList()
        }

        val split = parameters.split("&")
        split.map {
            val keyValue = it.split(delimiter)
            Pair(keyValue[0], keyValue[1])
        }
    } ?: emptyList()
}