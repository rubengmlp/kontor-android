package com.rubengmlp.kontor.core.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rubengmlp.kontor.core.domain.model.ThemeMode
import com.rubengmlp.kontor.core.domain.model.UserPreferences
import com.rubengmlp.kontor.core.domain.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

private val Context.userPreferencesDataStore by preferencesDataStore(name = "kontor_preferences")

class DataStoreUserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : UserPreferencesRepository {

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val biometricEnabled = booleanPreferencesKey("biometric_enabled")
        val hideBalances = booleanPreferencesKey("hide_balances")
    }

    override val preferences: Flow<UserPreferences> = context.userPreferencesDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { values ->
            UserPreferences(
                themeMode = values[Keys.themeMode]
                    ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
                    ?: ThemeMode.SYSTEM,
                biometricEnabled = values[Keys.biometricEnabled] ?: true,
                hideBalances = values[Keys.hideBalances] ?: false,
            )
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.userPreferencesDataStore.edit { it[Keys.themeMode] = mode.name }
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { it[Keys.biometricEnabled] = enabled }
    }

    override suspend fun setHideBalances(hidden: Boolean) {
        context.userPreferencesDataStore.edit { it[Keys.hideBalances] = hidden }
    }
}
