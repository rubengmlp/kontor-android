package com.rubengmlp.kontor.feature.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rubengmlp.kontor.core.designsystem.components.AmountText
import com.rubengmlp.kontor.core.designsystem.components.EmptyState
import com.rubengmlp.kontor.core.designsystem.components.KontorCard
import com.rubengmlp.kontor.core.designsystem.components.KontorDivider
import com.rubengmlp.kontor.core.designsystem.components.KontorScreenBackground
import com.rubengmlp.kontor.core.designsystem.components.KontorTopBar
import com.rubengmlp.kontor.core.designsystem.components.LabelValueRow
import com.rubengmlp.kontor.core.designsystem.components.LoadingState
import com.rubengmlp.kontor.core.designsystem.components.StatusPill
import com.rubengmlp.kontor.core.designsystem.theme.KontorTheme
import com.rubengmlp.kontor.core.domain.model.Transaction
import com.rubengmlp.kontor.core.domain.model.TransactionStatus
import com.rubengmlp.kontor.core.domain.money.Money

@Composable
fun TransactionDetailRoute(
    onBack: () -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TransactionDetailScreen(
        state = state,
        onBack = onBack,
    )
}

@Composable
fun TransactionDetailScreen(
    state: TransactionDetailUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KontorScreenBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            KontorTopBar(title = "Transaction", onBack = onBack)
            when {
                state.isLoading -> LoadingState("Loading transaction")
                state.transaction == null -> EmptyState(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    title = "Transaction not found",
                    message = "It may have been removed or is no longer available.",
                    actionText = "Go back",
                    onAction = onBack,
                )
                else -> TransactionDetailContent(state.transaction, state)
            }
        }
    }
}

@Composable
private fun TransactionDetailContent(
    transaction: Transaction,
    state: TransactionDetailUiState,
) {
    val amountColor = if (transaction.isIncome) {
        KontorTheme.colors.positive
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CategoryBadge(transaction.category, size = 68.dp)
        Spacer(Modifier.height(18.dp))
        AmountText(
            amount = Money.formatSigned(transaction.amountCents),
            hidden = false,
            style = MaterialTheme.typography.displayMedium,
            color = amountColor,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = transaction.title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        val counterparty = transaction.counterparty
        if (counterparty != null && counterparty != transaction.title) {
            Text(
                text = counterparty,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(12.dp))
        val pending = transaction.status == TransactionStatus.PENDING
        StatusPill(
            text = if (pending) "Pending" else "Booked",
            containerColor = if (pending) {
                KontorTheme.colors.pending.copy(alpha = 0.14f)
            } else {
                KontorTheme.colors.positive.copy(alpha = 0.13f)
            },
            contentColor = if (pending) KontorTheme.colors.pending else KontorTheme.colors.positive,
        )
        Spacer(Modifier.height(28.dp))
        KontorCard(modifier = Modifier.fillMaxWidth()) {
            LabelValueRow(
                label = "Date",
                value = transaction.bookedAt.asFullDate(),
            )
            KontorDivider()
            LabelValueRow(
                label = "Account",
                value = state.account?.let { "${it.name}\n${it.maskedIban}" } ?: "—",
            )
            KontorDivider()
            LabelValueRow(
                label = "Category",
                value = transaction.category.label(),
            )
            KontorDivider()
            LabelValueRow(
                label = "Reference",
                value = transaction.reference ?: "—",
            )
            val note = transaction.note
            if (note != null) {
                KontorDivider()
                LabelValueRow(label = "Note", value = note)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = if (pending) {
                "Pending card payments can still change. The final amount is confirmed when the payment is booked."
            } else {
                "This transaction is booked. Demo data only — no real money is involved."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Spacer(Modifier.height(28.dp))
    }
}
