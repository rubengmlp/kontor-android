package com.rubengmlp.kontor.feature.accounts

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionDetailViewModelTest {

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
    fun `loads the transaction and its account`() = runTest(dispatcher) {
        val viewModel = TransactionDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("id" to "tx_income")),
            repository = FakeBankRepository(),
        )

        viewModel.state.test {
            awaitItem()
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals("tx_income", state.transaction?.id)
            assertEquals("Business Giro", state.account?.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `missing transaction resolves to not found`() = runTest(dispatcher) {
        val viewModel = TransactionDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("id" to "missing")),
            repository = FakeBankRepository(),
        )

        viewModel.state.test {
            awaitItem()
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertNotNull(state)
            assertEquals(null, state.transaction)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
