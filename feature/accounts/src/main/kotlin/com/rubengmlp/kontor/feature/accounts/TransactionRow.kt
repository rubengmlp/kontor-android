package com.rubengmlp.kontor.feature.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.automirrored.outlined.CallReceived
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rubengmlp.kontor.core.designsystem.components.AmountText
import com.rubengmlp.kontor.core.designsystem.components.IconBadge
import com.rubengmlp.kontor.core.designsystem.components.StatusPill
import com.rubengmlp.kontor.core.designsystem.theme.KontorTheme
import com.rubengmlp.kontor.core.domain.model.Transaction
import com.rubengmlp.kontor.core.domain.model.TransactionCategory
import com.rubengmlp.kontor.core.domain.model.TransactionStatus
import com.rubengmlp.kontor.core.domain.money.Money
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.UK)
private val fullDateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale.UK)
private val groupDateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.UK)

fun Instant.asShortDate(): String =
    atZone(ZoneId.systemDefault()).format(shortDateFormatter)

fun Instant.asFullDate(): String =
    atZone(ZoneId.systemDefault()).format(fullDateFormatter)

fun LocalDate.asGroupHeader(): String = when (this) {
    LocalDate.now() -> "Today"
    LocalDate.now().minusDays(1) -> "Yesterday"
    else -> format(groupDateFormatter)
}

fun TransactionCategory.label(): String = when (this) {
    TransactionCategory.INCOME -> "Income"
    TransactionCategory.SOFTWARE -> "Software"
    TransactionCategory.DINING -> "Dining"
    TransactionCategory.TRAVEL -> "Travel"
    TransactionCategory.RENT -> "Rent"
    TransactionCategory.OFFICE -> "Office"
    TransactionCategory.TRANSFER -> "Transfer"
    TransactionCategory.TAX -> "Tax"
    TransactionCategory.OTHER -> "Other"
}

private data class CategoryVisual(
    val icon: ImageVector,
    val background: Color,
    val tint: Color,
)

@Composable
private fun categoryVisual(category: TransactionCategory): CategoryVisual {
    val extras = KontorTheme.colors
    return when (category) {
        TransactionCategory.INCOME -> CategoryVisual(
            Icons.AutoMirrored.Outlined.CallReceived,
            extras.positive.copy(alpha = 0.13f),
            extras.positive,
        )
        TransactionCategory.SOFTWARE -> CategoryVisual(
            Icons.Outlined.Code,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )
        TransactionCategory.DINING -> CategoryVisual(
            Icons.Outlined.Restaurant,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        TransactionCategory.TRAVEL -> CategoryVisual(
            Icons.Outlined.Flight,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        TransactionCategory.RENT -> CategoryVisual(
            Icons.Outlined.Apartment,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        TransactionCategory.OFFICE -> CategoryVisual(
            Icons.Outlined.BusinessCenter,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        TransactionCategory.TRANSFER -> CategoryVisual(
            Icons.Outlined.SwapHoriz,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        TransactionCategory.TAX -> CategoryVisual(
            Icons.Outlined.AccountBalance,
            extras.pending.copy(alpha = 0.14f),
            extras.pending,
        )
        TransactionCategory.OTHER -> CategoryVisual(
            Icons.AutoMirrored.Outlined.ReceiptLong,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun CategoryBadge(
    category: TransactionCategory,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val visual = categoryVisual(category)
    IconBadge(
        icon = visual.icon,
        contentDescription = category.label(),
        tint = visual.tint,
        background = visual.background,
        modifier = modifier,
        size = size,
    )
}

@Composable
fun TransactionRow(
    transaction: Transaction,
    hidden: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val amountColor = if (transaction.isIncome) {
        KontorTheme.colors.positive
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(transaction.category)
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = transaction.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
            )
            Text(
                text = "${transaction.category.label()} · ${transaction.bookedAt.asShortDate()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            AmountText(
                amount = Money.formatSigned(transaction.amountCents),
                hidden = hidden,
                style = MaterialTheme.typography.titleSmall,
                color = amountColor,
            )
            if (transaction.status == TransactionStatus.PENDING) {
                StatusPill(
                    text = "Pending",
                    containerColor = KontorTheme.colors.pending.copy(alpha = 0.14f),
                    contentColor = KontorTheme.colors.pending,
                )
            }
        }
    }
}
