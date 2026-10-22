package com.rubengmlp.kontor.core.domain.model

import java.time.Instant

data class TransferRequest(
    val fromAccountId: String,
    val recipientName: String,
    val iban: String,
    val amountCents: Long,
    val reference: String?,
)

data class TransferReceipt(
    val id: String,
    val bookedAt: Instant,
    val recipientName: String,
    val amountCents: Long,
)

data class TransferValidation(
    val recipientError: String? = null,
    val ibanError: String? = null,
    val amountError: String? = null,
    val referenceError: String? = null,
) {
    val isValid: Boolean
        get() = recipientError == null &&
            ibanError == null &&
            amountError == null &&
            referenceError == null
}
