package by.mksn.gae.easycurrbot.util

import by.mksn.gae.easycurrbot.AppConfig
import java.math.BigDecimal
import java.math.RoundingMode

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
