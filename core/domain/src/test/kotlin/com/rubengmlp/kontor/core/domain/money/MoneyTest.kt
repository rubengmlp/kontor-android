package com.rubengmlp.kontor.core.domain.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class MoneyTest {

    private fun String.normalized() = replace('\u00A0', ' ')

    @Test
    fun `formats cents as German euros`() {
        assertEquals("1.234,56 €", Money.format(123456).normalized())
        assertEquals("0,00 €", Money.format(0).normalized())
        assertEquals("-12,40 €", Money.format(-1240).normalized())
    }

    @Test
    fun `formats signed amounts`() {
        assertEquals("+1.234,56 €", Money.formatSigned(123456).normalized())
        assertEquals("-12,40 €", Money.formatSigned(-1240).normalized())
    }

    @Test
    fun `uses the requested locale`() {
        assertEquals("€1,234.56", Money.format(123456, Locale.UK))
    }

    @Test
    fun `parses comma decimals`() {
        assertEquals(123456L, Money.parseToCents("1234,56"))
        assertEquals(123456L, Money.parseToCents("1.234,56"))
        assertEquals(1234L, Money.parseToCents("12,34"))
    }

    @Test
    fun `parses dot decimals when exactly two digits follow`() {
        assertEquals(123456L, Money.parseToCents("1234.56"))
        assertEquals(123400L, Money.parseToCents("1.234"))
    }

    @Test
    fun `rejects empty, negative and malformed input`() {
        assertNull(Money.parseToCents(""))
        assertNull(Money.parseToCents("-10"))
        assertNull(Money.parseToCents("abc"))
    }

    @Test
    fun `sanitizes while typing`() {
        assertEquals("1234,5", Money.sanitizeInput("1.234,5"))
        assertEquals("12,34", Money.sanitizeInput("12,345"))
        assertEquals("12,", Money.sanitizeInput("12,"))
        assertEquals("0,5", Money.sanitizeInput(",5"))
        assertEquals("99", Money.sanitizeInput("a9b9"))
    }
}
