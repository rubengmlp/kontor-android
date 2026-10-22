package com.rubengmlp.kontor.feature.accounts

import app.cash.turbine.test
import com.rubengmlp.kontor.core.domain.model.Account
import com.rubengmlp.kontor.core.domain.model.AccountType
import com.rubengmlp.kontor.core.domain.model.ThemeMode
import com.rubengmlp.kontor.core.domain.model.Transaction
import com.rubengmlp.kontor.core.domain.model.TransactionCategory
import com.rubengmlp.kontor.core.domain.model.TransactionStatus
import com.rubengmlp.kontor.core.domain.model.TransferReceipt
import com.rubengmlp.kontor.core.domain.model.TransferRequest
import com.rubengmlp.kontor.core.domain.model.UserPreferences
import com.rubengmlp.kontor.core.domain.repository.BankRepository
import com.rubengmlp.kontor.core.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class FakeBankRepository(
    accounts: List<Account> = listOf(
        Account(
            id = "acc_giro",
            name = "Business Giro",
            iban = "DE89370400440532013000",
            balanceCents = 250_000,
            availableCents = 240_000,
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
    transactions: List<Transaction> = listOf(
        Transaction(
            id = "tx_income",
            accountId = "acc_giro",
            title = "Invoice · Studio Nord",
            counterparty = "Studio Nord",
            amountCents = 120_000,
            bookedAt = Instant.parse("2026-10-21T08:00:00Z"),
            category = TransactionCategory.INCOME,
            status = TransactionStatus.BOOKED,
            reference = "INV-1",
            note = null,
        ),
        Transaction(
            id = "tx_coffee",
            accountId = "acc_giro",
            title = "Coffee Fellow",
            counterparty = "Coffee Fellow",
            amountCents = -1_280,
            bookedAt = Instant.parse("2026-10-20T09:00:00Z"),
            category = TransactionCategory.DINING,
            status = TransactionStatus.PENDING,
            reference = null,
            note = "Team coffee",
        ),
        Transaction(
            id = "tx_savings",
            accountId = "acc_savings",
            title = "Transfer to reserve",
            counterparty = "Business Giro",
            amountCents = 500_000,
            bookedAt = Instant.parse("2026-10-19T09:00:00Z"),
            category = TransactionCategory.TRANSFER,
            status = TransactionStatus.BOOKED,
            reference = "SAV-1",
            note = null,
        ),
    ),
) : BankRepository {

    val accountsFlow = MutableStateFlow(accounts)
    val transactionsFlow = MutableStateFlow(transactions)
    var refreshResult: Result<Unit> = Result.success(Unit)
    var submitError: Throwable? = null
    var refreshCount: Int = 0

    override fun observeAccounts(): Flow<List<Account>> = accountsFlow

    override fun observeAccount(id: String): Flow<Account?> =
        accountsFlow.map { list -> list.firstOrNull { it.id == id } }

    override fun observeTransactions(): Flow<List<Transaction>> = transactionsFlow

    override fun observeRecentTransactions(limit: Int): Flow<List<Transaction>> =
        transactionsFlow.map { list -> list.take(limit) }

    override fun observeTransaction(id: String): Flow<Transaction?> =
        transactionsFlow.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun refresh(): Result<Unit> {
        refreshCount++
        return refreshResult
    }

    override suspend fun submitTransfer(request: TransferRequest): Result<TransferReceipt> {
        val error = submitError
        return if (error != null) {
            Result.failure(error)
        } else {
            Result.success(
                TransferReceipt(
                    id = "trf_1",
                    bookedAt = Instant.parse("2026-10-22T10:00:00Z"),
                    recipientName = request.recipientName,
                    amountCents = request.amountCents,
                ),
            )
        }
    }
}

class FakePreferencesRepository(
    initial: UserPreferences = UserPreferences(),
) : UserPreferencesRepository {

    val state = MutableStateFlow(initial)

    override val preferences: Flow<UserPreferences> = state

    override suspend fun setThemeMode(mode: ThemeMode) {
        state.update { it.copy(themeMode = mode) }
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        state.update { it.copy(biometricEnabled = enabled) }
    }

    override suspend fun setHideBalances(hidden: Boolean) {
        state.update { it.copy(hideBalances = hidden) }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AccountsViewModelTest {

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
    fun `combines total balance and recent transactions`() = runTest(dispatcher) {
        val repository = FakeBankRepository()
        val viewModel = AccountsViewModel(repository, FakePreferencesRepository())

        viewModel.state.test {
            awaitItem()
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertEquals(750_000L, state.totalBalanceCents)
            assertEquals(740_000L, state.availableCents)
            assertEquals(3, state.recentTransactions.size)
            assertEquals(1, repository.refreshCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggle balances persists through the preference repository`() = runTest(dispatcher) {
        val preferences = FakePreferencesRepository()
        val viewModel = AccountsViewModel(FakeBankRepository(), preferences)

        viewModel.state.test {
            awaitItem()
            advanceUntilIdle()
            expectMostRecentItem()

            viewModel.toggleBalances()
            advanceUntilIdle()
            assertTrue(expectMostRecentItem().hideBalances)
            assertTrue(preferences.state.value.hideBalances)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh failure is surfaced without hiding cached accounts`() = runTest(dispatcher) {
        val repository = FakeBankRepository()
        repository.refreshResult = Result.failure(IllegalStateException("Network down"))
        val viewModel = AccountsViewModel(repository, FakePreferencesRepository())

        viewModel.state.test {
            awaitItem()
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals("Network down", state.error)
            assertEquals(2, state.accounts.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `recent transactions are limited to six`() = runTest(dispatcher) {
        val many = (1..10).map { index ->
            Transaction(
                id = "tx_$index",
                accountId = "acc_giro",
                title = "Transaction $index",
                counterparty = null,
                amountCents = -index.toLong(),
                bookedAt = Instant.parse("2026-10-01T00:00:00Z").plusSeconds(index.toLong()),
                category = TransactionCategory.OTHER,
                status = TransactionStatus.BOOKED,
                reference = null,
                note = null,
            )
        }
        val viewModel = AccountsViewModel(
            FakeBankRepository(transactions = many),
            FakePreferencesRepository(),
        )

        viewModel.state.test {
            awaitItem()
            advanceUntilIdle()
            assertEquals(6, expectMostRecentItem().recentTransactions.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {

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
    fun `filters by query and direction`() = runTest(dispatcher) {
        val viewModel = TransactionsViewModel(FakeBankRepository(), FakePreferencesRepository())
        viewModel.state.test {
            awaitItem()
            advanceUntilIdle()
            expectMostRecentItem()

            viewModel.onQueryChange("coffee")
            advanceUntilIdle()
            val search = expectMostRecentItem()
            assertEquals(listOf("tx_coffee"), search.visibleTransactions.map { it.id })

            viewModel.onQueryChange("")
            viewModel.onFilterChange(TransactionFilter.IN)
            advanceUntilIdle()
            assertTrue(expectMostRecentItem().visibleTransactions.all { it.amountCents > 0 })

            viewModel.onFilterChange(TransactionFilter.OUT)
            advanceUntilIdle()
            assertTrue(expectMostRecentItem().visibleTransactions.all { it.amountCents < 0 })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clear filters restores every transaction`() = runTest(dispatcher) {
        val viewModel = TransactionsViewModel(FakeBankRepository(), FakePreferencesRepository())
        viewModel.state.test {
            awaitItem()
            advanceUntilIdle()
            expectMostRecentItem()

            viewModel.onQueryChange("does not exist")
            advanceUntilIdle()
            assertTrue(expectMostRecentItem().visibleTransactions.isEmpty())

            viewModel.clearFilters()
            advanceUntilIdle()
            assertEquals(3, expectMostRecentItem().visibleTransactions.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `searches counterparty and reference`() = runTest(dispatcher) {
        val viewModel = TransactionsViewModel(FakeBankRepository(), FakePreferencesRepository())
        viewModel.state.test {
            awaitItem()
            advanceUntilIdle()
            expectMostRecentItem()

            viewModel.onQueryChange("Studio")
            advanceUntilIdle()
            assertEquals(listOf("tx_income"), expectMostRecentItem().visibleTransactions.map { it.id })

            viewModel.onQueryChange("SAV-1")
            advanceUntilIdle()
            assertEquals(listOf("tx_savings"), expectMostRecentItem().visibleTransactions.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }
}
