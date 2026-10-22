package com.rubengmlp.kontor.core.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class AccountDto(
    val id: String,
    val name: String,
    val iban: String,
    val balanceCents: Long,
    val availableCents: Long,
    val type: String,
)

@Serializable
data class TransactionDto(
    val id: String,
    val accountId: String,
    val title: String,
    val counterparty: String? = null,
    val amountCents: Long,
    val bookedAt: String,
    val category: String,
    val status: String,
    val reference: String? = null,
    val note: String? = null,
)

@Serializable
data class TransferRequestDto(
    val fromAccountId: String,
    val recipientName: String,
    val iban: String,
    val amountCents: Long,
    val reference: String? = null,
)

@Serializable
data class TransferReceiptDto(
    val id: String,
    val bookedAt: String,
    val status: String,
)
