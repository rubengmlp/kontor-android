package com.rubengmlp.kontor.core.data.repository

import com.rubengmlp.kontor.core.domain.repository.BankRepository
import com.rubengmlp.kontor.core.domain.repository.UserPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBankRepository(implementation: DefaultBankRepository): BankRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(
        implementation: DataStoreUserPreferencesRepository,
    ): UserPreferencesRepository
}
