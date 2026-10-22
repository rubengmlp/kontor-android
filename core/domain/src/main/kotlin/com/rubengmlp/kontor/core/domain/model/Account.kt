package com.rubengmlp.kontor.core.domain.model

enum class AccountType {
    GIRO,
    SAVINGS,
    CREDIT,
}

data class Account(
    val id: String,
    val name: String,
    val iban: String,
    val balanceCents: Long,
    val availableCents: Long,
    val type: AccountType,
) {
    val maskedIban: String get() = "•••• ${iban.takeLast(4)}"
}
