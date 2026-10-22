package com.rubengmlp.kontor.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

/**
 * The balance update and the ledger entry must succeed or fail together.
 */
@Dao
abstract class TransferDao {

    @Query("UPDATE accounts SET balanceCents = :balanceCents, availableCents = :availableCents WHERE id = :accountId")
    abstract suspend fun updateBalance(
        accountId: String,
        balanceCents: Long,
        availableCents: Long,
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTransaction(transaction: TransactionEntity)

    @Transaction
    open suspend fun applyTransfer(
        accountId: String,
        balanceCents: Long,
        availableCents: Long,
        transaction: TransactionEntity,
    ) {
        updateBalance(accountId, balanceCents, availableCents)
        insertTransaction(transaction)
    }
}
