package com.rubengmlp.kontor.feature.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rubengmlp.kontor.core.designsystem.components.AmountText
import com.rubengmlp.kontor.core.designsystem.components.IconBadge
import com.rubengmlp.kontor.core.designsystem.components.KontorCard
import com.rubengmlp.kontor.core.designsystem.components.KontorDivider
import com.rubengmlp.kontor.core.designsystem.components.KontorScreenBackground
import com.rubengmlp.kontor.core.designsystem.components.KontorTopBar
import com.rubengmlp.kontor.core.designsystem.components.LabelValueRow
import com.rubengmlp.kontor.core.designsystem.components.PrimaryButton
import com.rubengmlp.kontor.core.designsystem.components.SecondaryButton
import com.rubengmlp.kontor.core.designsystem.components.StatusPill
import com.rubengmlp.kontor.core.designsystem.theme.KontorTheme
import com.rubengmlp.kontor.core.domain.money.Money
import com.rubengmlp.kontor.core.domain.validation.IbanValidator

@Composable
fun TransferRoute(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: TransferViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.startNewTransferIfComplete() }
    TransferScreen(
        state = state,
        onBack = onBack,
        onDone = onDone,
        onAccountSelected = viewModel::onAccountSelected,
        onRecipientChange = viewModel::onRecipientChange,
        onIbanChange = viewModel::onIbanChange,
        onAmountChange = viewModel::onAmountChange,
        onReferenceChange = viewModel::onReferenceChange,
        onUseBeneficiary = viewModel::useBeneficiary,
        onContinue = viewModel::onContinue,
        onEdit = viewModel::onEdit,
        onConfirm = viewModel::onConfirm,
        onDismissError = viewModel::dismissError,
    )
}

@Composable
fun TransferScreen(
    state: TransferUiState,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onAccountSelected: (String) -> Unit,
    onRecipientChange: (String) -> Unit,
    onIbanChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onReferenceChange: (String) -> Unit,
    onUseBeneficiary: (SampleBeneficiary) -> Unit,
    onContinue: () -> Unit,
    onEdit: () -> Unit,
    onConfirm: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.step == TransferStep.SUCCESS) {
        TransferSuccessScreen(state = state, onDone = onDone, modifier = modifier)
        return
    }

    KontorScreenBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            KontorTopBar(
                title = if (state.step == TransferStep.REVIEW) "Review transfer" else "New transfer",
                onBack = if (state.step == TransferStep.REVIEW) onEdit else onBack,
            )
            TransferProgress(step = state.step)
            when (state.step) {
                TransferStep.FORM -> TransferForm(
                    state = state,
                    onAccountSelected = onAccountSelected,
                    onRecipientChange = onRecipientChange,
                    onIbanChange = onIbanChange,
                    onAmountChange = onAmountChange,
                    onReferenceChange = onReferenceChange,
                    onUseBeneficiary = onUseBeneficiary,
                    onContinue = onContinue,
                    onDismissError = onDismissError,
                )
                TransferStep.REVIEW -> TransferReview(
                    state = state,
                    onConfirm = onConfirm,
                    onEdit = onEdit,
                    onDismissError = onDismissError,
                )
                TransferStep.SUCCESS -> Unit
            }
        }
    }
}

@Composable
private fun TransferProgress(step: TransferStep) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        ProgressStep("Details", number = 1, active = true, done = step != TransferStep.FORM)
        ProgressConnector(active = step != TransferStep.FORM)
        ProgressStep("Review", number = 2, active = step != TransferStep.FORM, done = false)
        ProgressConnector(active = false)
        ProgressStep("Done", number = 3, active = false, done = false)
    }
}

@Composable
private fun ProgressStep(label: String, number: Int, active: Boolean, done: Boolean) {
    val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val content = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(14.dp),
                )
            } else {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = content,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RowScope.ProgressConnector(active: Boolean) {
    Box(
        modifier = Modifier
            .weight(1f)
            .padding(top = 11.dp, start = 8.dp, end = 8.dp)
            .height(2.dp)
            .background(
                if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            ),
    )
}

@Composable
private fun TransferForm(
    state: TransferUiState,
    onAccountSelected: (String) -> Unit,
    onRecipientChange: (String) -> Unit,
    onIbanChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onReferenceChange: (String) -> Unit,
    onUseBeneficiary: (SampleBeneficiary) -> Unit,
    onContinue: () -> Unit,
    onDismissError: () -> Unit,
) {
    val recipientError = state.errors.recipientError
    val ibanError = state.errors.ibanError
    val amountError = state.errors.amountError
    val referenceError = state.errors.referenceError

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "From",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AccountSelector(state = state, onAccountSelected = onAccountSelected)
        Spacer(Modifier.height(2.dp))
        Text(
            text = "To",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            sampleBeneficiaries.forEach { beneficiary ->
                AssistChip(
                    onClick = { onUseBeneficiary(beneficiary) },
                    label = { Text(beneficiary.name) },
                )
            }
        }
        OutlinedTextField(
            value = state.recipientName,
            onValueChange = onRecipientChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Recipient name") },
            placeholder = { Text("e.g. Anna Weber") },
            singleLine = true,
            isError = recipientError != null,
            supportingText = if (recipientError != null) {
                { Text(recipientError) }
            } else {
                null
            },
            shape = MaterialTheme.shapes.medium,
            colors = fieldColors(),
        )
        OutlinedTextField(
            value = state.iban,
            onValueChange = onIbanChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("IBAN") },
            placeholder = { Text("DE89 3704 0044 0532 0130 00") },
            singleLine = true,
            isError = ibanError != null,
            supportingText = if (ibanError != null) {
                { Text(ibanError) }
            } else {
                { Text("Spaces are ignored. The checksum is validated locally.") }
            },
            shape = MaterialTheme.shapes.medium,
            colors = fieldColors(),
        )
        OutlinedTextField(
            value = state.amountInput,
            onValueChange = onAmountChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Amount") },
            prefix = {
                Text(
                    text = "€",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            textStyle = MaterialTheme.typography.headlineSmall,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = amountError != null,
            supportingText = {
                Text(
                    text = amountError
                        ?: "${Money.format(state.selectedAccount?.availableCents ?: 0)} available",
                    color = if (amountError != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            },
            shape = MaterialTheme.shapes.medium,
            colors = fieldColors(),
        )
        OutlinedTextField(
            value = state.reference,
            onValueChange = onReferenceChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Reference (optional)") },
            placeholder = { Text("Invoice 1042") },
            singleLine = true,
            isError = referenceError != null,
            supportingText = if (referenceError != null) {
                { Text(referenceError) }
            } else {
                { Text("${state.reference.length}/140") }
            },
            shape = MaterialTheme.shapes.medium,
            colors = fieldColors(),
        )
        if (state.submitError != null) {
            ErrorNote(message = state.submitError, onDismiss = onDismissError)
        }
        PrimaryButton(
            text = "Review transfer",
            onClick = onContinue,
            enabled = state.accounts.isNotEmpty(),
            icon = Icons.Outlined.SwapHoriz,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Lock,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Demo transfer · no real money moves",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AccountSelector(
    state: TransferUiState,
    onAccountSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val account = state.selectedAccount
    Box {
        KontorCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { expanded = true },
            contentPadding = PaddingValues(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    icon = Icons.Outlined.AccountBalance,
                    contentDescription = null,
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account?.name ?: "Select an account",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = account?.maskedIban ?: "No accounts available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Available",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = Money.format(account?.availableCents ?: 0),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = "Choose account")
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            state.accounts.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(option.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${option.maskedIban} · ${Money.format(option.availableCents)} available",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        onAccountSelected(option.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun TransferReview(
    state: TransferUiState,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    onDismissError: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(14.dp))
        Text(
            text = "You're sending",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        AmountText(
            amount = Money.format(state.amountCents ?: 0),
            hidden = false,
            style = MaterialTheme.typography.displayMedium,
        )
        Text(
            text = "to ${state.recipientName}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        KontorCard(modifier = Modifier.fillMaxWidth()) {
            LabelValueRow(
                label = "From",
                value = state.selectedAccount?.let { "${it.name}\n${it.maskedIban}" } ?: "—",
            )
            KontorDivider()
            LabelValueRow(
                label = "IBAN",
                value = IbanValidator.format(state.iban),
            )
            KontorDivider()
            LabelValueRow(
                label = "Reference",
                value = state.reference.ifBlank { "—" },
            )
            KontorDivider()
            LabelValueRow(label = "Fee", value = "Free")
            KontorDivider()
            LabelValueRow(label = "Arrives", value = "Instantly · demo")
        }
        if (state.submitError != null) {
            Spacer(Modifier.height(12.dp))
            ErrorNote(message = state.submitError, onDismiss = onDismissError)
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = "Confirm transfer",
            onClick = onConfirm,
            loading = state.isSubmitting,
            icon = Icons.Outlined.Lock,
        )
        Spacer(Modifier.height(10.dp))
        SecondaryButton(
            text = "Edit details",
            onClick = onEdit,
            enabled = !state.isSubmitting,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Check the recipient carefully. Bank transfers cannot be recalled once they are booked.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun TransferSuccessScreen(
    state: TransferUiState,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val receipt = state.receipt
    val amount = receipt?.amountCents ?: state.amountCents ?: 0L
    KontorScreenBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.7f))
            Surface(
                shape = CircleShape,
                color = KontorTheme.colors.positive.copy(alpha = 0.14f),
                modifier = Modifier.size(96.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = KontorTheme.colors.positive,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = "Transfer sent",
                style = MaterialTheme.typography.displaySmall,
            )
            Spacer(Modifier.height(6.dp))
            AmountText(
                amount = Money.format(amount),
                hidden = false,
                style = MaterialTheme.typography.displayMedium,
                color = KontorTheme.colors.positive,
            )
            Text(
                text = "to ${receipt?.recipientName ?: state.recipientName}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            KontorCard(modifier = Modifier.fillMaxWidth()) {
                LabelValueRow(
                    label = "From",
                    value = state.selectedAccount?.name ?: "Business Giro",
                )
                KontorDivider()
                LabelValueRow(
                    label = "Reference",
                    value = state.reference.ifBlank { "—" },
                )
                KontorDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    StatusPill(
                        text = "Booked",
                        containerColor = KontorTheme.colors.positive.copy(alpha = 0.13f),
                        contentColor = KontorTheme.colors.positive,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            PrimaryButton(text = "Back to accounts", onClick = onDone)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "This is a demo. No real money was moved.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ErrorNote(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "Dismiss",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable(onClick = onDismiss)
                .padding(6.dp),
        )
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
)
