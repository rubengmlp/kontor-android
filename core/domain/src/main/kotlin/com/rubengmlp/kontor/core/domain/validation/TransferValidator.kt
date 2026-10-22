package com.rubengmlp.kontor.core.domain.validation

import com.rubengmlp.kontor.core.domain.model.TransferValidation

object TransferValidator {

    fun validate(
        recipientName: String,
        iban: String,
        amountCents: Long?,
        availableCents: Long,
        reference: String,
    ): TransferValidation {
        val name = recipientName.trim()
        return TransferValidation(
            recipientError = when {
                name.isEmpty() -> "Enter the recipient's name"
                name.length > 70 -> "Name is too long"
                else -> null
            },
            ibanError = if (IbanValidator.isValid(iban)) null else "Enter a valid IBAN",
            amountError = when {
                amountCents == null || amountCents <= 0L -> "Enter an amount greater than zero"
                amountCents > availableCents -> "Amount exceeds the available balance"
                else -> null
            },
            referenceError = if (reference.length > 140) "Reference is too long" else null,
        )
    }
}
