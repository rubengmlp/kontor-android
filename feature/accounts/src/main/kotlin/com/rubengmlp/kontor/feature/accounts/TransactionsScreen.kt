package com.rubengmlp.kontor.feature.accounts

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rubengmlp.kontor.core.designsystem.components.EmptyState
import com.rubengmlp.kontor.core.designsystem.components.ErrorState
import com.rubengmlp.kontor.core.designsystem.components.KontorCard
import com.rubengmlp.kontor.core.designsystem.components.KontorScreenBackground
import com.rubengmlp.kontor.core.designsystem.components.KontorTopBar
import com.rubengmlp.kontor.core.designsystem.components.LoadingState
import com.rubengmlp.kontor.core.designsystem.components.PrivacyToggle
import com.rubengmlp.kontor.core.domain.model.Transaction
import java.time.ZoneId

@Composable
fun TransactionsRoute(
    onTransactionClick: (String) -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TransactionsScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onQueryChange = viewModel::onQueryChange,
        onFilterChange = viewModel::onFilterChange,
        onClearFilters = viewModel::clearFilters,
        onToggleBalances = viewModel::toggleBalances,
        onTransactionClick = onTransactionClick,
    )
}

@Composable
fun TransactionsScreen(
    state: TransactionsUiState,
    onRefresh: () -> Unit,
    onQueryChange: (String) -> Unit,
    onFilterChange: (TransactionFilter) -> Unit,
    onClearFilters: () -> Unit,
    onToggleBalances: () -> Unit,
    onTransactionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    KontorScreenBackground(modifier = modifier) {
        when {
            state.isLoading && state.transactions.isEmpty() -> LoadingState("Loading your activity")
            state.error != null && state.transactions.isEmpty() -> ErrorState(
                message = state.error ?: "Could not load your activity",
                onRetry = onRefresh,
            )
            else -> Column(modifier = Modifier.fillMaxSize()) {
                KontorTopBar(
                    title = "Activity",
                    subtitle = "All accounts",
                    actions = {
                        PrivacyToggle(hidden = state.hideBalances, onToggle = onToggleBalances)
                        IconButton(onClick = onRefresh, enabled = !state.isRefreshing) {
                            if (state.isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(Icons.Outlined.Refresh, contentDescription = "Refresh activity")
                            }
                        }
                    },
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        SearchField(
                            query = state.query,
                            onQueryChange = onQueryChange,
                            onClear = { onQueryChange("") },
                        )
                    }
                    item {
                        FilterRow(selected = state.filter, onSelect = onFilterChange)
                    }
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 4.dp),
                        ) {
                            Text(
                                text = "${state.visibleTransactions.size} transactions",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "Newest first",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (state.error != null) {
                        item {
                            KontorCard(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                            ) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = state.error ?: "Could not refresh your activity",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.weight(1f),
                                    )
                                    TextButton(onClick = onRefresh) { Text("Retry") }
                                }
                            }
                        }
                    }
                    when {
                        state.transactions.isEmpty() -> item {
                            EmptyState(
                                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                                title = "No activity yet",
                                message = "Incoming and outgoing payments will appear here.",
                            )
                        }
                        state.visibleTransactions.isEmpty() -> item {
                            EmptyState(
                                icon = Icons.Outlined.Search,
                                title = "No matching transactions",
                                message = "Try a different search or clear the filters.",
                                actionText = "Clear filters",
                                onAction = onClearFilters,
                            )
                        }
                        else -> transactionGroups(state.visibleTransactions).forEach { (date, transactions) ->
                            item(key = "header_$date") {
                                Text(
                                    text = date.asGroupHeader(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 14.dp, bottom = 2.dp),
                                )
                            }
                            items(transactions, key = { it.id }) { transaction ->
                                TransactionRow(
                                    transaction = transaction,
                                    hidden = state.hideBalances,
                                    onClick = { onTransactionClick(transaction.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun transactionGroups(transactions: List<Transaction>) =
    transactions.groupBy { it.bookedAt.atZone(ZoneId.systemDefault()).toLocalDate() }

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Search merchant, reference…") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = onClear) {
                    Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                }
            }
        } else {
            null
        },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        ),
    )
}

@Composable
private fun FilterRow(
    selected: TransactionFilter,
    onSelect: (TransactionFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            TransactionFilter.ALL to "All",
            TransactionFilter.IN to "Money in",
            TransactionFilter.OUT to "Money out",
        ).forEach { (filter, label) ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = {
                    Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}
