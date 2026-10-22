package com.rubengmlp.kontor.feature.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rubengmlp.kontor.core.designsystem.components.AmountText
import com.rubengmlp.kontor.core.designsystem.components.EmptyState
import com.rubengmlp.kontor.core.designsystem.components.ErrorState
import com.rubengmlp.kontor.core.designsystem.components.HeroCard
import com.rubengmlp.kontor.core.designsystem.components.KontorCard
import com.rubengmlp.kontor.core.designsystem.components.KontorScreenBackground
import com.rubengmlp.kontor.core.designsystem.components.LoadingState
import com.rubengmlp.kontor.core.designsystem.components.PrivacyToggle
import com.rubengmlp.kontor.core.designsystem.components.SectionHeader
import com.rubengmlp.kontor.core.designsystem.theme.KontorTheme
import com.rubengmlp.kontor.core.domain.model.Account
import com.rubengmlp.kontor.core.domain.model.AccountType
import com.rubengmlp.kontor.core.domain.model.Transaction
import com.rubengmlp.kontor.core.domain.money.Money
import java.time.LocalTime

@Composable
fun AccountsRoute(
    onSeeAllTransactions: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onTransfer: () -> Unit,
    viewModel: AccountsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AccountsScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onToggleBalances = viewModel::toggleBalances,
        onSeeAllTransactions = onSeeAllTransactions,
        onTransactionClick = onTransactionClick,
        onTransfer = onTransfer,
    )
}

@Composable
fun AccountsScreen(
    state: AccountsUiState,
    onRefresh: () -> Unit,
    onToggleBalances: () -> Unit,
    onSeeAllTransactions: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onTransfer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KontorScreenBackground(modifier = modifier) {
        when {
            state.isLoading && state.accounts.isEmpty() -> LoadingState()
            state.error != null && state.accounts.isEmpty() -> ErrorState(
                message = state.error ?: "Could not load your accounts",
                onRetry = onRefresh,
            )
            else -> AccountsContent(
                state = state,
                onRefresh = onRefresh,
                onToggleBalances = onToggleBalances,
                onSeeAllTransactions = onSeeAllTransactions,
                onTransactionClick = onTransactionClick,
                onTransfer = onTransfer,
            )
        }
    }
}

@Composable
private fun AccountsContent(
    state: AccountsUiState,
    onRefresh: () -> Unit,
    onToggleBalances: () -> Unit,
    onSeeAllTransactions: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onTransfer: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            HomeHeader(
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
            )
        }
        item {
            HeroCard(
                modifier = Modifier.padding(horizontal = 20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Total balance",
                        style = MaterialTheme.typography.labelLarge,
                        color = KontorTheme.colors.heroMuted,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onToggleBalances) {
                        Icon(
                            imageVector = if (state.hideBalances) {
                                Icons.Outlined.VisibilityOff
                            } else {
                                Icons.Outlined.Visibility
                            },
                            contentDescription = if (state.hideBalances) "Show balances" else "Hide balances",
                            tint = KontorTheme.colors.onHero,
                        )
                    }
                }
                AmountText(
                    amount = Money.format(state.totalBalanceCents),
                    hidden = state.hideBalances,
                    style = MaterialTheme.typography.displayLarge,
                    color = KontorTheme.colors.onHero,
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.12f),
                        contentColor = KontorTheme.colors.onHero,
                    ) {
                        Text(
                            text = "${state.accounts.size} accounts",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                    Text(
                        text = if (state.hideBalances) {
                            "Available balance hidden"
                        } else {
                            "${Money.format(state.availableCents)} available"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = KontorTheme.colors.heroMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                QuickAction(
                    label = "New transfer",
                    icon = Icons.Outlined.SwapHoriz,
                    onClick = onTransfer,
                    modifier = Modifier.weight(1f),
                    primary = true,
                )
                QuickAction(
                    label = "Activity",
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    onClick = onSeeAllTransactions,
                    modifier = Modifier.weight(1f),
                    primary = false,
                )
            }
        }
        if (state.error != null) {
            item {
                InlineError(
                    message = state.error ?: "Could not refresh your accounts",
                    onRetry = onRefresh,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
        item {
            SectionHeader(
                title = "Your accounts",
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
        item {
            if (state.accounts.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.AccountBalance,
                    title = "No accounts yet",
                    message = "Your business accounts will appear here once they are available.",
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.accounts, key = { it.id }) { account ->
                        AccountCard(account = account, hidden = state.hideBalances)
                    }
                }
            }
        }
        item {
            SectionHeader(
                title = "Recent activity",
                actionText = "See all",
                onAction = onSeeAllTransactions,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
        if (state.recentTransactions.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    title = "No activity yet",
                    message = "Payments in and out will show up here.",
                )
            }
        } else {
            items(state.recentTransactions, key = { it.id }) { transaction ->
                TransactionRow(
                    transaction = transaction,
                    hidden = state.hideBalances,
                    onClick = { onTransactionClick(transaction.id) },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(isRefreshing: Boolean, onRefresh: () -> Unit) {
    val greeting = remember {
        when (LocalTime.now().hour) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Alex Morgan",
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        IconButton(onClick = onRefresh, enabled = !isRefreshing) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(Icons.Outlined.Refresh, contentDescription = "Refresh accounts")
            }
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    brush = Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                    ),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "AM",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean,
) {
    val container = if (primary) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    val content = if (primary) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Surface(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = content,
        border = if (primary) null else androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun AccountCard(account: Account, hidden: Boolean) {
    val extras = KontorTheme.colors
    val (background, content) = when (account.type) {
        AccountType.GIRO -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        AccountType.SAVINGS -> extras.cardSage to extras.onCardSage
        AccountType.CREDIT -> extras.cardInk to extras.onCardInk
    }
    val icon = when (account.type) {
        AccountType.GIRO -> Icons.Outlined.AccountBalance
        AccountType.SAVINGS -> Icons.Outlined.Savings
        AccountType.CREDIT -> Icons.Outlined.CreditCard
    }
    Surface(
        modifier = Modifier
            .width(272.dp)
            .height(164.dp),
        shape = MaterialTheme.shapes.large,
        color = background,
        contentColor = content,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = content.copy(alpha = 0.14f),
                    contentColor = content,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(18.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = account.maskedIban,
                        style = MaterialTheme.typography.labelMedium,
                        color = content.copy(alpha = 0.72f),
                    )
                }
            }
            Column {
                Text(
                    text = if (account.type == AccountType.CREDIT) "Amount due" else "Balance",
                    style = MaterialTheme.typography.labelMedium,
                    color = content.copy(alpha = 0.72f),
                )
                AmountText(
                    amount = Money.format(account.balanceCents),
                    hidden = hidden,
                    style = MaterialTheme.typography.headlineMedium,
                    color = content,
                )
            }
        }
    }
}

@Composable
private fun InlineError(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    KontorCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            androidx.compose.material3.TextButton(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}
