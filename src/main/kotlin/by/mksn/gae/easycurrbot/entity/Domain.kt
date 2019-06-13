package by.mksn.gae.easycurrbot.entity

import java.math.BigDecimal
import java.math.RoundingMode


enum class ExpressionType {
    SINGLE_VALUE, MULTI_CURRENCY_EXPR, SINGLE_CURRENCY_EXPR
}

data class RawInputQuery(
        val type: ExpressionType,
        val expression: String,
        val expressionResult: BigDecimal,
        val baseCurrency: String,
        val involvedCurrencies: List<String>,
        val addCurrencies: List<String>,
        val removeCurrencies: List<String>
)

data class InputQuery(
        val rawInput: String,
        val type: ExpressionType,
        val expression: String,
        val expressionResult: BigDecimal,
        val baseCurrency: String,
        val involvedCurrencies: List<String>,
        val targets: List<String>
) {
    fun isOneUnit() = type == ExpressionType.SINGLE_VALUE && expression == "1"
}


data class InputError(
        val rawInput: String,
        val errorPosition: Int,
        val message: String
) {
    fun toMarkdown() = """
        $message (at $errorPosition)
        ```  ${"▼".padStart(if (errorPosition > rawInput.length) rawInput.length else errorPosition)}
        > $rawInput
          ${"▲".padStart(if (errorPosition > rawInput.length) rawInput.length else errorPosition)}```
    """.trimIndent()

    fun toSingleLine() = "(at $errorPosition) $rawInput"
}

fun String.trimToLength(n: Int, tail: String = "") =
        if (this.length <= n) this else this.take(n - tail.length) + tail

data class Currency(val code: String, val symbol: String, val aliases: List<String>)

fun Currency.toOneUnitInputQuery(internalPrecision: Int, targets: List<String>) = InputQuery(
        rawInput = "1 $code",
        type = ExpressionType.SINGLE_VALUE,
        expression = "1",
        expressionResult = 1.toBigDecimal().setScale(internalPrecision, RoundingMode.HALF_UP),
        baseCurrency = code,
        involvedCurrencies = listOf(code),
        targets = targets
)

data class ExchangedSum(val currency: Currency, val sum: BigDecimal)

data class ExchangeResults(val input: InputQuery, val rates: List<ExchangedSum>)

sealed class Result<out V, out E> {

    open operator fun component1(): V? = null
    open operator fun component2(): E? = null

    inline fun <X> fold(success: (V) -> X, failure: (E) -> X): X = when (this) {
        is Success -> success(this.value)
        is Failure -> failure(this.error)
    }

    abstract fun get(): V

    class Success<out V : Any>(val value: V) : Result<V, Nothing>() {
        override fun component1(): V? = value

        override fun get(): V = value

        override fun toString() = "[Success: $value]"

        override fun hashCode(): Int = value.hashCode()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Success<*> && value == other.value
        }
    }

    class Failure<out E : Any>(val error: E) : Result<Nothing, E>() {
        override fun component2(): E? = error

        override fun get() = throw IllegalStateException("Cannot retrieve success result from Failure: $error")

        override fun toString() = "[Failure: \"$error\"]"

        override fun hashCode(): Int = error.hashCode()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Failure<*> && error == other.error
        }
    }

    companion object {
        fun <E : Any> failure(v: E) = Failure(v)
        fun <V : Any> success(v: V) = Success(v)
    }

}

inline fun <V : Any> Result<V, *>.success(f: (V) -> Unit) = fold(f, {})

inline fun <E : Any> Result<*, E>.failure(f: (E) -> Unit) = fold({}, f)
