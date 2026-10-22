package com.rubengmlp.kontor.core.domain.model

import java.time.Instant

enum class TransactionCategory {
    INCOME,
    SOFTWARE,
    DINING,
    TRAVEL,
    RENT,
    OFFICE,
    TRANSFER,
    TAX,
    OTHER,
}

enum class TransactionStatus {
    BOOKED,
    PENDING,
}

data class Transaction(
    val id: String,
    val accountId: String,
    val title: String,
    val counterparty: String?,
    val amountCents: Long,
    val bookedAt: Instant,
    val category: TransactionCategory,
    val status: TransactionStatus,
    val reference: String?,
    val note: String?,
) {
    val isIncome: Boolean get() = amountCents > 0
}
