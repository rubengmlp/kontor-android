package com.rubengmlp.kontor.core.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferValidatorTest {

    private val validIban = "DE89 3704 0044 0532 0130 00"

    @Test
    fun `accepts a valid transfer`() {
        val result = TransferValidator.validate(
            recipientName = "Anna Weber",
            iban = validIban,
            amountCents = 25_000,
            availableCents = 100_000,
            reference = "Invoice 42",
        )
        assertTrue(result.isValid)
        assertNull(result.amountError)
    }

    @Test
    fun `rejects an empty recipient`() {
        val result = TransferValidator.validate("   ", validIban, 100, 1000, "")
        assertEquals("Enter the recipient's name", result.recipientError)
        assertFalse(result.isValid)
    }

    @Test
    fun `rejects an invalid IBAN`() {
        val result = TransferValidator.validate("Anna", "DE00 0000", 100, 1000, "")
        assertEquals("Enter a valid IBAN", result.ibanError)
    }

    @Test
    fun `rejects zero and negative amounts`() {
        assertEquals(
            "Enter an amount greater than zero",
            TransferValidator.validate("Anna", validIban, 0, 1000, "").amountError,
        )
        assertEquals(
            "Enter an amount greater than zero",
            TransferValidator.validate("Anna", validIban, -1, 1000, "").amountError,
        )
        assertEquals(
            "Enter an amount greater than zero",
            TransferValidator.validate("Anna", validIban, null, 1000, "").amountError,
        )
    }

    @Test
    fun `rejects an amount above the available balance`() {
        val result = TransferValidator.validate("Anna", validIban, 1001, 1000, "")
        assertEquals("Amount exceeds the available balance", result.amountError)
    }

    @Test
    fun `rejects a long reference`() {
        val result = TransferValidator.validate("Anna", validIban, 100, 1000, "x".repeat(141))
        assertEquals("Reference is too long", result.referenceError)
    }
}
