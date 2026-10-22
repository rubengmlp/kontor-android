package com.rubengmlp.kontor.core.domain.repository

import com.rubengmlp.kontor.core.domain.model.ThemeMode
import com.rubengmlp.kontor.core.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {

    val preferences: Flow<UserPreferences>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setBiometricEnabled(enabled: Boolean)

    suspend fun setHideBalances(hidden: Boolean)
}
