package com.rubengmlp.kontor.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rubengmlp.kontor.core.domain.model.Account
import com.rubengmlp.kontor.core.domain.model.AccountType

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iban: String,
    val balanceCents: Long,
    val availableCents: Long,
    val type: String,
)

fun AccountEntity.toDomain(): Account = Account(
    id = id,
    name = name,
    iban = iban,
    balanceCents = balanceCents,
    availableCents = availableCents,
    type = AccountType.entries.firstOrNull { it.name == type } ?: AccountType.GIRO,
)

fun Account.toEntity(): AccountEntity = AccountEntity(
    id = id,
    name = name,
    iban = iban,
    balanceCents = balanceCents,
    availableCents = availableCents,
    type = type.name,
)
