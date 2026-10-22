package com.rubengmlp.kontor.feature.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rubengmlp.kontor.core.domain.model.Account
import com.rubengmlp.kontor.core.domain.model.AccountType
import com.rubengmlp.kontor.core.domain.model.TransferReceipt
import com.rubengmlp.kontor.core.domain.model.TransferRequest
import com.rubengmlp.kontor.core.domain.model.TransferValidation
import com.rubengmlp.kontor.core.domain.money.Money
import com.rubengmlp.kontor.core.domain.repository.BankRepository
import com.rubengmlp.kontor.core.domain.validation.IbanValidator
import com.rubengmlp.kontor.core.domain.validation.TransferValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TransferStep {
    FORM,
    REVIEW,
    SUCCESS,
}

data class SampleBeneficiary(
    val name: String,
    val iban: String,
)

val sampleBeneficiaries = listOf(
    SampleBeneficiary("Anna Weber", "DE89 3704 0044 0532 0130 00"),
    SampleBeneficiary("Studio Nord", "DE02 1203 0000 0000 2020 51"),
    SampleBeneficiary("Weber GmbH", "DE44 5001 0517 5407 3249 31"),
)

data class TransferUiState(
    val accounts: List<Account> = emptyList(),
    val fromAccountId: String? = null,
    val recipientName: String = "",
    val iban: String = "",
    val amountInput: String = "",
    val reference: String = "",
    val step: TransferStep = TransferStep.FORM,
    val errors: TransferValidation = TransferValidation(),
    val isSubmitting: Boolean = false,
    val receipt: TransferReceipt? = null,
    val submitError: String? = null,
) {
    val selectedAccount: Account?
        get() = accounts.firstOrNull { it.id == fromAccountId }

    val amountCents: Long?
        get() = Money.parseToCents(amountInput)
}

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val repository: BankRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TransferUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAccounts().collect { accounts ->
                _state.update { current ->
                    val existing = current.fromAccountId?.takeIf { id -> accounts.any { it.id == id } }
                    val preferred = accounts.firstOrNull { it.type == AccountType.GIRO }?.id
                        ?: accounts.firstOrNull()?.id
                    current.copy(
                        accounts = accounts,
                        fromAccountId = existing ?: preferred,
                    )
                }
            }
        }
    }

    fun onAccountSelected(accountId: String) {
        _state.update { it.copy(fromAccountId = accountId, submitError = null) }
    }

    fun onRecipientChange(value: String) {
        _state.update {
            it.copy(
                recipientName = value.take(70),
                errors = it.errors.copy(recipientError = null),
                submitError = null,
            )
        }
    }

    fun onIbanChange(value: String) {
        val filtered = value.uppercase().filter { it.isLetterOrDigit() || it.isWhitespace() }
        _state.update {
            it.copy(
                iban = filtered.take(42),
                errors = it.errors.copy(ibanError = null),
                submitError = null,
            )
        }
    }

    fun onAmountChange(value: String) {
        _state.update {
            it.copy(
                amountInput = Money.sanitizeInput(value),
                errors = it.errors.copy(amountError = null),
                submitError = null,
            )
        }
    }

    fun onReferenceChange(value: String) {
        _state.update {
            it.copy(
                reference = value.take(140),
                errors = it.errors.copy(referenceError = null),
                submitError = null,
            )
        }
    }

    fun useBeneficiary(beneficiary: SampleBeneficiary) {
        _state.update {
            it.copy(
                recipientName = beneficiary.name,
                iban = beneficiary.iban,
                errors = it.errors.copy(recipientError = null, ibanError = null),
                submitError = null,
            )
        }
    }

    fun onContinue() {
        val current = _state.value
        val account = current.selectedAccount
        if (account == null) {
            _state.update { it.copy(submitError = "Select an account first") }
            return
        }
        val validation = TransferValidator.validate(
            recipientName = current.recipientName,
            iban = current.iban,
            amountCents = current.amountCents,
            availableCents = account.availableCents,
            reference = current.reference,
        )
        if (!validation.isValid) {
            _state.update { it.copy(errors = validation, submitError = null) }
            return
        }
        _state.update {
            it.copy(
                step = TransferStep.REVIEW,
                errors = TransferValidation(),
                submitError = null,
            )
        }
    }

    fun onEdit() {
        _state.update { it.copy(step = TransferStep.FORM, submitError = null) }
    }

    fun onConfirm() {
        val current = _state.value
        val account = current.selectedAccount ?: return
        val amount = current.amountCents ?: return
        if (current.isSubmitting) return

        _state.update { it.copy(isSubmitting = true, submitError = null) }
        viewModelScope.launch {
            val result = repository.submitTransfer(
                TransferRequest(
                    fromAccountId = account.id,
                    recipientName = current.recipientName.trim(),
                    iban = IbanValidator.normalize(current.iban),
                    amountCents = amount,
                    reference = current.reference.trim().ifBlank { null },
                ),
            )
            result.fold(
                onSuccess = { receipt ->
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            step = TransferStep.SUCCESS,
                            receipt = receipt,
                            submitError = null,
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            submitError = error.message ?: "The transfer could not be completed",
                        )
                    }
                },
            )
        }
    }

    fun dismissError() {
        _state.update { it.copy(submitError = null) }
    }

    /**
     * A completed receipt should not be shown again when the user returns to the tab.
     * Drafts are intentionally kept; only the terminal success state is cleared.
     */
    fun startNewTransferIfComplete() {
        _state.update { current ->
            if (current.step != TransferStep.SUCCESS) {
                current
            } else {
                TransferUiState(
                    accounts = current.accounts,
                    fromAccountId = current.fromAccountId,
                )
            }
        }
    }
}
