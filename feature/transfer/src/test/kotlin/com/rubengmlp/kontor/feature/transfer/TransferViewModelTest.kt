package com.rubengmlp.kontor.feature.transfer

import com.rubengmlp.kontor.core.domain.model.Account
import com.rubengmlp.kontor.core.domain.model.AccountType
import com.rubengmlp.kontor.core.domain.model.Transaction
import com.rubengmlp.kontor.core.domain.model.TransferReceipt
import com.rubengmlp.kontor.core.domain.model.TransferRequest
import com.rubengmlp.kontor.core.domain.repository.BankRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

private class TransferFakeRepository(
    accounts: List<Account> = listOf(
        Account(
            id = "acc_giro",
            name = "Business Giro",
            iban = "DE89370400440532013000",
            balanceCents = 100_000,
            availableCents = 90_000,
            type = AccountType.GIRO,
        ),
        Account(
            id = "acc_savings",
            name = "Reserve Account",
            iban = "DE02120300000000202051",
            balanceCents = 500_000,
            availableCents = 500_000,
            type = AccountType.SAVINGS,
        ),
    ),
) : BankRepository {

    val accountsFlow = MutableStateFlow(accounts)
    var submitError: Throwable? = null
    var lastRequest: TransferRequest? = null

    override fun observeAccounts(): Flow<List<Account>> = accountsFlow

    override fun observeAccount(id: String): Flow<Account?> =
        accountsFlow.map { list -> list.firstOrNull { it.id == id } }

    override fun observeTransactions(): Flow<List<Transaction>> = MutableStateFlow(emptyList())

    override fun observeRecentTransactions(limit: Int): Flow<List<Transaction>> =
        MutableStateFlow(emptyList())

    override fun observeTransaction(id: String): Flow<Transaction?> = MutableStateFlow(null)

    override suspend fun refresh(): Result<Unit> = Result.success(Unit)

    override suspend fun submitTransfer(request: TransferRequest): Result<TransferReceipt> {
        lastRequest = request
        val error = submitError
        return if (error != null) {
            Result.failure(error)
        } else {
            Result.success(
                TransferReceipt(
                    id = "trf_test",
                    bookedAt = Instant.parse("2026-10-22T10:00:00Z"),
                    recipientName = request.recipientName,
                    amountCents = request.amountCents,
                ),
            )
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TransferViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `selects the giro account by default`() = runTest(dispatcher) {
        val viewModel = TransferViewModel(TransferFakeRepository())
        advanceUntilIdle()

        assertEquals("acc_giro", viewModel.state.value.fromAccountId)
        assertEquals("Business Giro", viewModel.state.value.selectedAccount?.name)
    }

    @Test
    fun `valid details move to review and submit creates a receipt`() = runTest(dispatcher) {
        val repository = TransferFakeRepository()
        val viewModel = TransferViewModel(repository)
        advanceUntilIdle()

        viewModel.onRecipientChange("Anna Weber")
        viewModel.onIbanChange("DE89 3704 0044 0532 0130 00")
        viewModel.onAmountChange("25,00")
        viewModel.onReferenceChange("Invoice 42")
        viewModel.onContinue()

        assertEquals(TransferStep.REVIEW, viewModel.state.value.step)
        assertTrue(viewModel.state.value.errors.isValid)

        viewModel.onConfirm()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(TransferStep.SUCCESS, state.step)
        assertNotNull(state.receipt)
        assertEquals(2_500L, repository.lastRequest?.amountCents)
        assertEquals("DE89370400440532013000", repository.lastRequest?.iban)
        assertEquals("Invoice 42", repository.lastRequest?.reference)
    }

    @Test
    fun `invalid form stays on details and exposes field errors`() = runTest(dispatcher) {
        val viewModel = TransferViewModel(TransferFakeRepository())
        advanceUntilIdle()

        viewModel.onAmountChange("1.000,00")
        viewModel.onContinue()

        val state = viewModel.state.value
        assertEquals(TransferStep.FORM, state.step)
        assertEquals("Enter the recipient's name", state.errors.recipientError)
        assertEquals("Enter a valid IBAN", state.errors.ibanError)
        assertEquals("Amount exceeds the available balance", state.errors.amountError)
    }

    @Test
    fun `a sample beneficiary fills recipient and iban`() = runTest(dispatcher) {
        val viewModel = TransferViewModel(TransferFakeRepository())
        advanceUntilIdle()

        viewModel.useBeneficiary(sampleBeneficiaries.first())

        assertEquals("Anna Weber", viewModel.state.value.recipientName)
        assertEquals("DE89 3704 0044 0532 0130 00", viewModel.state.value.iban)
    }

    @Test
    fun `submission failure is shown and keeps the review state`() = runTest(dispatcher) {
        val repository = TransferFakeRepository()
        repository.submitError = IllegalStateException("Backend unavailable")
        val viewModel = TransferViewModel(repository)
        advanceUntilIdle()

        viewModel.onRecipientChange("Anna Weber")
        viewModel.onIbanChange("DE89 3704 0044 0532 0130 00")
        viewModel.onAmountChange("10,00")
        viewModel.onContinue()
        viewModel.onConfirm()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(TransferStep.REVIEW, state.step)
        assertEquals("Backend unavailable", state.submitError)
        assertNull(state.receipt)
    }

    @Test
    fun `a completed transfer is cleared when the screen is opened again`() = runTest(dispatcher) {
        val viewModel = TransferViewModel(TransferFakeRepository())
        advanceUntilIdle()

        viewModel.onRecipientChange("Anna Weber")
        viewModel.onIbanChange("DE89 3704 0044 0532 0130 00")
        viewModel.onAmountChange("10,00")
        viewModel.onContinue()
        viewModel.onConfirm()
        advanceUntilIdle()
        assertEquals(TransferStep.SUCCESS, viewModel.state.value.step)

        viewModel.startNewTransferIfComplete()

        val state = viewModel.state.value
        assertEquals(TransferStep.FORM, state.step)
        assertEquals("", state.recipientName)
        assertNull(state.receipt)
        assertEquals("acc_giro", state.fromAccountId)
    }

    @Test
    fun `switching account changes the available balance used for validation`() = runTest(dispatcher) {
        val viewModel = TransferViewModel(TransferFakeRepository())
        advanceUntilIdle()

        viewModel.onRecipientChange("Anna Weber")
        viewModel.onIbanChange("DE89 3704 0044 0532 0130 00")
        viewModel.onAmountChange("1.000,00")
        viewModel.onContinue()
        assertEquals("Amount exceeds the available balance", viewModel.state.value.errors.amountError)

        viewModel.onAccountSelected("acc_savings")
        viewModel.onContinue()
        assertEquals(TransferStep.REVIEW, viewModel.state.value.step)
    }
}
