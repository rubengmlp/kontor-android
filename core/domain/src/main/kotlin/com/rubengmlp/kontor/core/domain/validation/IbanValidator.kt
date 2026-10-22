package com.rubengmlp.kontor.core.domain.validation

/**
 * IBAN validation using the ISO 13616 mod-97 checksum.
 */
object IbanValidator {

    private val shape = Regex("[A-Z]{2}\\d{2}[A-Z0-9]+")

    fun normalize(raw: String): String =
        raw.uppercase().filterNot { it.isWhitespace() || it == '-' }

    fun isValid(raw: String): Boolean {
        val iban = normalize(raw)
        if (iban.length !in 15..34 || !shape.matches(iban)) return false

        val rearranged = iban.drop(4) + iban.take(4)
        var remainder = 0
        for (character in rearranged) {
            val value = when (character) {
                in '0'..'9' -> character - '0'
                in 'A'..'Z' -> character - 'A' + 10
                else -> return false
            }
            remainder = if (value < 10) {
                (remainder * 10 + value) % 97
            } else {
                (remainder * 100 + value) % 97
            }
        }
        return remainder == 1
    }

    fun format(raw: String): String = normalize(raw).chunked(4).joinToString(" ")
}
