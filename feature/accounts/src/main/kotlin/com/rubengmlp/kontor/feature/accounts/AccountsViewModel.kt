package com.rubengmlp.kontor.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rubengmlp.kontor.core.domain.model.Account
import com.rubengmlp.kontor.core.domain.model.Transaction
import com.rubengmlp.kontor.core.domain.repository.BankRepository
import com.rubengmlp.kontor.core.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val totalBalanceCents: Long = 0L,
    val availableCents: Long = 0L,
    val hideBalances: Boolean = false,
    val error: String? = null,
)

private data class AccountsLocalState(
    val initialLoad: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val repository: BankRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val local = MutableStateFlow(AccountsLocalState())

    val state = combine(
        repository.observeAccounts(),
        repository.observeRecentTransactions(6),
        preferencesRepository.preferences,
        local,
    ) { accounts, transactions, preferences, localState ->
        AccountsUiState(
            isLoading = localState.initialLoad && accounts.isEmpty(),
            isRefreshing = localState.isRefreshing,
            accounts = accounts,
            recentTransactions = transactions,
            totalBalanceCents = accounts.sumOf { it.balanceCents },
            availableCents = accounts.sumOf { it.availableCents },
            hideBalances = preferences.hideBalances,
            error = localState.error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AccountsUiState(),
    )

    init {
        refresh()
    }

    fun refresh() {
        if (local.value.isRefreshing) return
        viewModelScope.launch {
            local.update { it.copy(isRefreshing = true, error = null) }
            val result = repository.refresh()
            local.update {
                it.copy(
                    initialLoad = false,
                    isRefreshing = false,
                    error = result.exceptionOrNull()?.let { error ->
                        error.message ?: "Could not refresh your accounts"
                    },
                )
            }
        }
    }

    fun toggleBalances() {
        val current = state.value.hideBalances
        viewModelScope.launch { preferencesRepository.setHideBalances(!current) }
    }
}
