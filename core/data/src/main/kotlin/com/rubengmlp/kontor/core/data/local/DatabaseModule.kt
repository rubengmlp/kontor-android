package com.rubengmlp.kontor.core.data.local

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KontorDatabase =
        Room.databaseBuilder(context, KontorDatabase::class.java, "kontor.db")
            .build()

    @Provides
    fun provideAccountDao(database: KontorDatabase): AccountDao = database.accountDao()

    @Provides
    fun provideTransactionDao(database: KontorDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideTransferDao(database: KontorDatabase): TransferDao = database.transferDao()
}
