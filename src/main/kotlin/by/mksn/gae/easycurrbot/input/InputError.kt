package by.mksn.gae.easycurrbot.input

import by.mksn.gae.easycurrbot.AppConfig
import com.github.h0tk3y.betterParse.parser.*

data class InputError(
        val rawInput: String,
        val errorPosition: Int,
        val message: String
)

private fun String.escapeMarkdown() = replace("*", "\\*")
        .replace("_", "\\_")
        .replace("`", "\\`")

fun String.trimToLength(n: Int, tail: String = "") =
        if (this.length <= n) this else this.take(kotlin.math.max(n - tail.length, 0)) + tail

fun InputError.toMarkdown() = """
        ${message.escapeMarkdown()} (at $errorPosition)
        ```  ${"▼".padStart(if (errorPosition > rawInput.length) rawInput.length else errorPosition)}
        > $rawInput
          ${"▲".padStart(if (errorPosition > rawInput.length) rawInput.length else errorPosition)}```
    """.trimIndent()

fun InputError.toSingleLine() = "(at $errorPosition) $rawInput"

fun ErrorResult.toInputError(rawInput: String, config: AppConfig): InputError = when (this) {
    is UnparsedRemainder -> InputError(
            rawInput = rawInput.trimToLength(config.telegram.outputWidthChars, tail = "…"),
            errorPosition = startsWith.column,
            message = if (startsWith.type.name == config.strings.tokenNames.currency) config.strings.errors.illegalCurrencyPlacement else config.strings.errors.unparsedReminder
    )
    is MismatchedToken -> InputError(
            rawInput = rawInput.trimToLength(config.telegram.outputWidthChars, tail = "…"),
            errorPosition = found.column,
            message = config.strings.errors.mismatchedToken.format(found.text, expected.name)
    )
    is NoMatchingToken -> InputError(
            rawInput = rawInput.trimToLength(config.telegram.outputWidthChars, tail = "…"),
            errorPosition = tokenMismatch.column,
            message = config.strings.errors.invalidCurrencyAlias.format(tokenMismatch.text)
    )
    is UnexpectedEof -> InputError(
            rawInput = rawInput.trimToLength(config.telegram.outputWidthChars, tail = "…"),
            errorPosition = rawInput.trim().length,
            message = config.strings.errors.unexpectedEOF.format(expected.name)
    )
    is AlternativesFailure -> {
        fun find(errors: List<ErrorResult>): ErrorResult {
            val error = errors.last()
            return if (error !is AlternativesFailure) error else find(error.errors)
        }
        find(errors).toInputError(rawInput, config)
    }
    else -> InputError(
            rawInput = rawInput.trimToLength(config.telegram.outputWidthChars, tail = "…"),
            errorPosition = rawInput.trim().length,
            message = config.strings.errors.unexpectedError
    )
}