package by.mksn.gae.easycurrbot.entity

/**
 * @author Mikhail Snitavets
 */
enum class UpdateType(val fieldName: String) {
    Message("message"),
    EditedMessage("edited_message"),
    ChannelPost("channel_post"),
    EditedChannelPost("edited_channel_post"),
    InlineQuery("inline_query"),
    ChosenInlineResult("chosen_inline_result"),
    CallbackQuery("callback_query"),
    ShippingQuery("shipping_query"),
    PreCheckoutQuery("pre_checkout_query");
}

fun Update.getType() = when {
    message != null -> UpdateType.Message
    editedMessage != null -> UpdateType.EditedMessage
    channelPost != null -> UpdateType.ChannelPost
    editedChannelPost != null -> UpdateType.EditedChannelPost
    inlineQuery != null -> UpdateType.InlineQuery
    chosenInlineResult != null -> UpdateType.ChosenInlineResult
    callbackQuery != null -> UpdateType.CallbackQuery
    shippingQuery != null -> UpdateType.ShippingQuery
    preCheckoutQuery != null -> UpdateType.PreCheckoutQuery
    else -> throw IllegalStateException("Invalid update type")
}

fun String.isUpdateType() = UpdateType.values().filter { this == it.fieldName }.any()