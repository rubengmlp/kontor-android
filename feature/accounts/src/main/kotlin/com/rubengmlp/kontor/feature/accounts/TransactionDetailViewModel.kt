package com.rubengmlp.kontor.feature.accounts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rubengmlp.kontor.core.domain.model.Account
import com.rubengmlp.kontor.core.domain.model.Transaction
import com.rubengmlp.kontor.core.domain.repository.BankRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class TransactionDetailUiState(
    val isLoading: Boolean = true,
    val transaction: Transaction? = null,
    val account: Account? = null,
)

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: BankRepository,
) : ViewModel() {

    private val transactionId: String = checkNotNull(savedStateHandle["id"]) {
        "TransactionDetail requires an id argument"
    }

    val state = combine(
        repository.observeTransaction(transactionId),
        repository.observeAccounts(),
    ) { transaction, accounts ->
        TransactionDetailUiState(
            isLoading = false,
            transaction = transaction,
            account = accounts.firstOrNull { it.id == transaction?.accountId },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionDetailUiState(),
    )
}
