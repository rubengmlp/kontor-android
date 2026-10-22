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

enum class TransactionFilter {
    ALL,
    IN,
    OUT,
}

private data class FilterState(
    val query: String = "",
    val filter: TransactionFilter = TransactionFilter.ALL,
)

private data class TransactionsLocalState(
    val initialLoad: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

data class TransactionsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val transactions: List<Transaction> = emptyList(),
    val visibleTransactions: List<Transaction> = emptyList(),
    val accountsById: Map<String, Account> = emptyMap(),
    val query: String = "",
    val filter: TransactionFilter = TransactionFilter.ALL,
    val hideBalances: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val repository: BankRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val filters = MutableStateFlow(FilterState())
    private val local = MutableStateFlow(TransactionsLocalState())

    val state = combine(
        repository.observeTransactions(),
        repository.observeAccounts(),
        preferencesRepository.preferences,
        filters,
        local,
    ) { transactions, accounts, preferences, filterState, localState ->
        val query = filterState.query.trim()
        val visible = transactions
            .filter { transaction ->
                query.isEmpty() ||
                    transaction.title.contains(query, ignoreCase = true) ||
                    transaction.counterparty?.contains(query, ignoreCase = true) == true ||
                    transaction.reference?.contains(query, ignoreCase = true) == true
            }
            .filter { transaction ->
                when (filterState.filter) {
                    TransactionFilter.ALL -> true
                    TransactionFilter.IN -> transaction.amountCents > 0
                    TransactionFilter.OUT -> transaction.amountCents < 0
                }
            }

        TransactionsUiState(
            isLoading = localState.initialLoad && transactions.isEmpty(),
            isRefreshing = localState.isRefreshing,
            transactions = transactions,
            visibleTransactions = visible,
            accountsById = accounts.associateBy { it.id },
            query = filterState.query,
            filter = filterState.filter,
            hideBalances = preferences.hideBalances,
            error = localState.error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionsUiState(),
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
                        error.message ?: "Could not refresh your activity"
                    },
                )
            }
        }
    }

    fun onQueryChange(query: String) {
        filters.update { it.copy(query = query) }
    }

    fun onFilterChange(filter: TransactionFilter) {
        filters.update { it.copy(filter = filter) }
    }

    fun clearFilters() {
        filters.value = FilterState()
    }

    fun toggleBalances() {
        val current = state.value.hideBalances
        viewModelScope.launch { preferencesRepository.setHideBalances(!current) }
    }
}
