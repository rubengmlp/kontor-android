package com.rubengmlp.kontor.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rubengmlp.kontor.core.domain.model.Transaction
import com.rubengmlp.kontor.core.domain.model.TransactionCategory
import com.rubengmlp.kontor.core.domain.model.TransactionStatus
import java.time.Instant

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val title: String,
    val counterparty: String?,
    val amountCents: Long,
    val bookedAt: Long,
    val category: String,
    val status: String,
    val reference: String?,
    val note: String?,
)

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    accountId = accountId,
    title = title,
    counterparty = counterparty,
    amountCents = amountCents,
    bookedAt = Instant.ofEpochMilli(bookedAt),
    category = TransactionCategory.entries.firstOrNull { it.name == category } ?: TransactionCategory.OTHER,
    status = TransactionStatus.entries.firstOrNull { it.name == status } ?: TransactionStatus.BOOKED,
    reference = reference,
    note = note,
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    accountId = accountId,
    title = title,
    counterparty = counterparty,
    amountCents = amountCents,
    bookedAt = bookedAt.toEpochMilli(),
    category = category.name,
    status = status.name,
    reference = reference,
    note = note,
)
