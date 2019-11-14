package by.mksn.gae.easycurrbot.extensions

import by.mksn.gae.easycurrbot.AppConfig
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.max

/**
 * Trims the string to the specified [n] number of chars with optional [tail]
 */
fun String.trimToLength(n: Int, tail: String = "") =
        if (this.length <= n) this else this.take(max(n - tail.length, 0)) + tail

/**
 * Returns a BigDecimal whose scale is the specified in [AppConfig]
 */
fun BigDecimal.toConfScale(config: AppConfig): BigDecimal = this.setScale(config.currencies.internalPrecision, RoundingMode.HALF_UP)

/**
 * Converts the string to [BigDecimal] with the scale as specified in [AppConfig]
 */
fun String.toConfScaledBigDecimal(config: AppConfig): BigDecimal = toBigDecimal().toConfScale(config)

/**
 * Converts the integer to [BigDecimal] with the scale as specified in [AppConfig]
 */
fun Int.toConfScaledBigDecimal(config: AppConfig): BigDecimal = toBigDecimal().toConfScale(config)
