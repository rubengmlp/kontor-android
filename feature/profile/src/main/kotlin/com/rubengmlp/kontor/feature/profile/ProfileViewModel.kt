package com.rubengmlp.kontor.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rubengmlp.kontor.core.domain.model.ThemeMode
import com.rubengmlp.kontor.core.domain.model.UserPreferences
import com.rubengmlp.kontor.core.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val preferences: UserPreferences = UserPreferences(),
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val state = preferencesRepository.preferences
        .map { ProfileUiState(isLoading = false, preferences = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProfileUiState(),
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setBiometricEnabled(enabled) }
    }

    fun setHideBalances(hidden: Boolean) {
        viewModelScope.launch { preferencesRepository.setHideBalances(hidden) }
    }
}
