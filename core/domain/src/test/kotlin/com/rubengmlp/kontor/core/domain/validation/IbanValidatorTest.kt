package com.rubengmlp.kontor.core.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IbanValidatorTest {

    @Test
    fun `accepts known valid IBANs`() {
        assertTrue(IbanValidator.isValid("DE89 3704 0044 0532 0130 00"))
        assertTrue(IbanValidator.isValid("GB82 WEST 1234 5698 7654 32"))
        assertTrue(IbanValidator.isValid("FR14 2004 1010 0505 0001 3M02 606"))
    }

    @Test
    fun `is case insensitive`() {
        assertTrue(IbanValidator.isValid("de89370400440532013000"))
    }

    @Test
    fun `rejects a broken checksum`() {
        assertFalse(IbanValidator.isValid("DE89 3704 0044 0532 0130 01"))
    }

    @Test
    fun `rejects malformed values`() {
        assertFalse(IbanValidator.isValid(""))
        assertFalse(IbanValidator.isValid("DE89"))
        assertFalse(IbanValidator.isValid("DE89 3704 0044 0532 0130 0X"))
        assertFalse(IbanValidator.isValid("1234 5678 9012 3456"))
    }

    @Test
    fun `normalizes and formats`() {
        assertEquals("DE89370400440532013000", IbanValidator.normalize("de89 3704 0044 0532 0130 00"))
        assertEquals("DE89 3704 0044 0532 0130 00", IbanValidator.format("de89370400440532013000"))
    }
}
