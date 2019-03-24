package by.mksn.gae.easycurrbot.entity

import java.lang.IllegalStateException
import java.math.BigDecimal
import java.math.RoundingMode


//sealed class InputBaseQuery(
//        val rawQuery: String,
//        val base: String,
//        val targets: List<String>
//)
//
//class SingleCurrencyInputQuery(
//        rawQuery: String,
//        base: String,
//        targets: List<String>,
//        val sumExpression: String,
//        val sum: BigDecimal
//) : InputBaseQuery(rawQuery, base, targets)
//
//class MultiCurrencyInputQuery(
//        rawQuery: String,
//        base: String,
//        targets: List<String>,
//        val sumExpression: String,
//        val sumValues: List<>
//) : InputBaseQuery(rawQuery, base, targets)

data class InputQuery(
        val rawQuery: String,
        val sumExpression: String,
        val sum: BigDecimal,
        val base: String,
        val targets: List<String>
)

data class Currency(val code: String, val symbol: String, val matchPatterns: List<String>)

fun Currency.toOneUnitInputQuery(internalPrecision: Int, targets: List<String>) =
        InputQuery("1 $code", "1", 1.toBigDecimal().setScale(internalPrecision, RoundingMode.HALF_EVEN), code, targets)

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

        override fun get() = throw IllegalStateException("Cannot retrieve success result from Failure")

        override fun toString() = "[Failure: \"$error\"]"

        override fun hashCode(): Int = error.hashCode()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Failure<*> && error == other.error
        }
    }

    companion object {
        fun error(message: String) = Failure(message)
        fun <V : Any> success(v: V) = Success(v)
    }

}

inline fun <V : Any> Result<V, *>.success(f: (V) -> Unit) = fold(f, {})

inline fun <E : Any> Result<*, E>.failure(f: (E) -> Unit) = fold({}, f)
