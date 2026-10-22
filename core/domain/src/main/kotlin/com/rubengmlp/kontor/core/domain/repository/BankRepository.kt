package com.rubengmlp.kontor.core.domain.repository

import com.rubengmlp.kontor.core.domain.model.Account
import com.rubengmlp.kontor.core.domain.model.Transaction
import com.rubengmlp.kontor.core.domain.model.TransferReceipt
import com.rubengmlp.kontor.core.domain.model.TransferRequest
import kotlinx.coroutines.flow.Flow

interface BankRepository {

    fun observeAccounts(): Flow<List<Account>>

    fun observeAccount(id: String): Flow<Account?>

    fun observeTransactions(): Flow<List<Transaction>>

    fun observeRecentTransactions(limit: Int): Flow<List<Transaction>>

    fun observeTransaction(id: String): Flow<Transaction?>

    suspend fun refresh(): Result<Unit>

    suspend fun submitTransfer(request: TransferRequest): Result<TransferReceipt>
}
