package com.rubengmlp.kontor.core.domain.money

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * Money is always handled as integer cents. Floats and doubles are never used for balances.
 */
object Money {

    fun format(cents: Long, locale: Locale = Locale.GERMANY): String =
        NumberFormat.getCurrencyInstance(locale).apply {
            currency = java.util.Currency.getInstance("EUR")
        }.format(BigDecimal.valueOf(cents).movePointLeft(2))

    fun formatSigned(cents: Long, locale: Locale = Locale.GERMANY): String {
        val formatted = format(cents, locale)
        return if (cents > 0) "+$formatted" else formatted
    }

    /**
     * Accepts "1234", "1234,56" and "1234.56". A dot followed by exactly two digits is read
     * as a decimal separator; otherwise it is treated as a thousands separator.
     */
    fun parseToCents(input: String): Long? {
        val cleaned = input.trim().replace("€", "").replace(" ", "").replace("\u00A0", "")
        if (cleaned.isEmpty()) return null
        val normalized = when {
            cleaned.contains(',') -> cleaned.replace(".", "").replace(',', '.')
            cleaned.matches(Regex("\\d+\\.\\d{2}")) -> cleaned
            else -> cleaned.replace(".", "")
        }
        val value = normalized.toBigDecimalOrNull() ?: return null
        if (value < BigDecimal.ZERO) return null
        return runCatching {
            value.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
        }.getOrNull()
    }

    /**
     * Keeps one decimal separator (always displayed as a comma) and at most two decimals.
     * Both "1.234,5" and "1,234.5" are treated as 1234.5, so a German or an English
     * numeric keyboard produces the same value.
     */
    fun sanitizeInput(raw: String): String {
        val filtered = raw.filter { it.isDigit() || it == ',' || it == '.' }.take(15)
        val separatorIndex = filtered.indexOfLast { it == ',' || it == '.' }
        if (separatorIndex < 0) return filtered

        val wholeDigits = filtered.take(separatorIndex).filter { it.isDigit() }
        val whole = wholeDigits.trimStart('0').ifEmpty { "0" }
        val decimals = filtered.drop(separatorIndex + 1)
            .filter { it.isDigit() }
            .take(2)
        return if (decimals.isEmpty()) "$whole," else "$whole,$decimals"
    }
}
