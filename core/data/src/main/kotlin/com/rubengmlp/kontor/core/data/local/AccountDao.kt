package com.rubengmlp.kontor.core.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query(
        """
        SELECT * FROM accounts
        ORDER BY CASE type WHEN 'GIRO' THEN 0 WHEN 'SAVINGS' THEN 1 ELSE 2 END, name
        """,
    )
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun observeById(id: String): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: String): AccountEntity?

    @Query("SELECT * FROM accounts")
    suspend fun getAll(): List<AccountEntity>

    @Upsert
    suspend fun upsertAll(accounts: List<AccountEntity>)
}
