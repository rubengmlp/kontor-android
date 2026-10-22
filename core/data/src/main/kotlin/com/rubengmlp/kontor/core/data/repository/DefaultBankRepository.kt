package com.rubengmlp.kontor.core.data.repository

import com.rubengmlp.kontor.core.data.local.AccountDao
import com.rubengmlp.kontor.core.data.local.TransactionDao
import com.rubengmlp.kontor.core.data.local.TransferDao
import com.rubengmlp.kontor.core.data.local.toDomain
import com.rubengmlp.kontor.core.data.local.toEntity
import com.rubengmlp.kontor.core.data.remote.AccountDto
import com.rubengmlp.kontor.core.data.remote.BankApi
import com.rubengmlp.kontor.core.data.remote.TransactionDto
import com.rubengmlp.kontor.core.data.remote.TransferRequestDto
import com.rubengmlp.kontor.core.domain.model.Account
import com.rubengmlp.kontor.core.domain.model.Transaction
import com.rubengmlp.kontor.core.domain.model.TransactionCategory
import com.rubengmlp.kontor.core.domain.model.TransactionStatus
import com.rubengmlp.kontor.core.domain.model.TransferReceipt
import com.rubengmlp.kontor.core.domain.model.TransferRequest
import com.rubengmlp.kontor.core.domain.repository.BankRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class DefaultBankRepository @Inject constructor(
    private val api: BankApi,
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val transferDao: TransferDao,
) : BankRepository {

    override fun observeAccounts(): Flow<List<Account>> =
        accountDao.observeAll().map { accounts -> accounts.map { it.toDomain() } }

    override fun observeAccount(id: String): Flow<Account?> =
        accountDao.observeById(id).map { it?.toDomain() }

    override fun observeTransactions(): Flow<List<Transaction>> =
        transactionDao.observeAll().map { transactions -> transactions.map { it.toDomain() } }

    override fun observeRecentTransactions(limit: Int): Flow<List<Transaction>> =
        transactionDao.observeRecent(limit).map { transactions -> transactions.map { it.toDomain() } }

    override fun observeTransaction(id: String): Flow<Transaction?> =
        transactionDao.observeById(id).map { it?.toDomain() }

    override suspend fun refresh(): Result<Unit> = resultOf {
        coroutineScope {
            val accounts = async { api.getAccounts() }
            val transactions = async { api.getTransactions() }

            val remoteAccounts = accounts.await()
            val localById = accountDao.getAll().associateBy { it.id }

            // The demo backend is static. Preserve balances that were changed by local
            // transfers; a real backend would be authoritative instead.
            val merged = remoteAccounts.map { dto ->
                val remote = dto.toEntity()
                localById[remote.id]?.let {
                    remote.copy(
                        balanceCents = it.balanceCents,
                        availableCents = it.availableCents,
                    )
                } ?: remote
            }

            accountDao.upsertAll(merged)
            transactionDao.upsertAll(transactions.await().map(TransactionDto::toEntity))
        }
    }

    override suspend fun submitTransfer(request: TransferRequest): Result<TransferReceipt> = resultOf {
        val account = accountDao.getById(request.fromAccountId)
            ?: error("The selected account no longer exists")
        require(request.amountCents > 0) { "Enter an amount greater than zero" }
        require(request.amountCents <= account.availableCents) { "Amount exceeds the available balance" }

        val receipt = api.postTransfer(request.toDto())
        val bookedAt = runCatching { Instant.parse(receipt.bookedAt) }
            .getOrElse { Instant.now() }

        transferDao.applyTransfer(
            accountId = account.id,
            balanceCents = account.balanceCents - request.amountCents,
            availableCents = account.availableCents - request.amountCents,
            transaction = Transaction(
                id = receipt.id,
                accountId = account.id,
                title = "Transfer to ${request.recipientName}",
                counterparty = request.recipientName,
                amountCents = -request.amountCents,
                bookedAt = bookedAt,
                category = TransactionCategory.TRANSFER,
                status = TransactionStatus.BOOKED,
                reference = request.reference,
                note = null,
            ).toEntity(),
        )

        TransferReceipt(
            id = receipt.id,
            bookedAt = bookedAt,
            recipientName = request.recipientName,
            amountCents = request.amountCents,
        )
    }
}

private fun AccountDto.toEntity() = com.rubengmlp.kontor.core.data.local.AccountEntity(
    id = id,
    name = name,
    iban = iban,
    balanceCents = balanceCents,
    availableCents = availableCents,
    type = type,
)

private fun TransactionDto.toEntity() = com.rubengmlp.kontor.core.data.local.TransactionEntity(
    id = id,
    accountId = accountId,
    title = title,
    counterparty = counterparty,
    amountCents = amountCents,
    bookedAt = runCatching { Instant.parse(bookedAt).toEpochMilli() }
        .getOrElse { Instant.now().toEpochMilli() },
    category = category,
    status = status,
    reference = reference,
    note = note,
)

private fun TransferRequest.toDto() = TransferRequestDto(
    fromAccountId = fromAccountId,
    recipientName = recipientName,
    iban = iban.filterNot { it.isWhitespace() },
    amountCents = amountCents,
    reference = reference,
)

private suspend fun <T> resultOf(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (error: Exception) {
    Result.failure(error)
}
