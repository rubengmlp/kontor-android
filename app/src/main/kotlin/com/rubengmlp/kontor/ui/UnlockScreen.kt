package com.rubengmlp.kontor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubengmlp.kontor.core.designsystem.components.KontorLogo
import com.rubengmlp.kontor.core.designsystem.components.StatusPill
import com.rubengmlp.kontor.core.designsystem.theme.KontorTheme
import kotlinx.coroutines.delay

private const val DEMO_PIN = "1234"
private const val MAX_PIN_LENGTH = 4

@Composable
fun UnlockScreen(
    biometricEnabled: Boolean,
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extras = KontorTheme.colors
    var pin by rememberSaveable { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var biometricMessage by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    val biometric = rememberBiometricUnlock(
        enabled = biometricEnabled,
        onSuccess = onUnlocked,
        onError = { message ->
            biometricMessage = message
        },
    )

    LaunchedEffect(pin) {
        if (pin.length == MAX_PIN_LENGTH) {
            if (pin == DEMO_PIN) {
                onUnlocked()
            } else {
                pinError = "That PIN doesn't match. Try 1234."
                delay(180)
                pin = ""
            }
        }
    }

    fun appendDigit(digit: String) {
        if (pin.length < MAX_PIN_LENGTH) {
            pin += digit
            pinError = null
            biometricMessage = null
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(extras.heroStart, extras.heroEnd)))
            .statusBarsPadding(),
        verticalArrangement = Arrangement.Bottom,
    ) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                UnlockHero(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp),
                )
            }
        }
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Welcome back",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f),
                            )
                            StatusPill(text = "Demo")
                        }
                        Text(
                            text = "Enter your 4-digit PIN to continue.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                        )
                        Spacer(Modifier.height(18.dp))
                        PinDots(
                            length = pin.length,
                            error = pinError != null,
                        )
                        if (pinError != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = pinError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                            )
                        }
                        if (biometric.isAvailable) {
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = {
                                    biometricMessage = null
                                    biometric.authenticate()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Fingerprint,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Use biometrics")
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                )
                                Text(
                                    text = "or use your PIN",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                )
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                )
                            }
                        }
                        if (!biometric.isAvailable && biometricEnabled) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Biometrics are not set up on this device. Use PIN 1234.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                        if (biometricMessage != null) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = biometricMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        PinPad(
                            onDigit = ::appendDigit,
                            onDelete = {
                                if (pin.isNotEmpty()) {
                                    pin = pin.dropLast(1)
                                    pinError = null
                                }
                            },
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Demo PIN: 1234 · no real money",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnlockHero(modifier: Modifier = Modifier) {
    val extras = KontorTheme.colors
    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KontorLogo(size = 42.dp, onDark = true)
            Spacer(Modifier.width(10.dp))
            Text(
                text = "KONTOR",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp,
                color = extras.onHero,
            )
            Spacer(Modifier.weight(1f))
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.12f),
                contentColor = extras.onHero,
            ) {
                Text(
                    text = "BUSINESS",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
        Spacer(Modifier.height(34.dp))
        Text(
            text = "Good to see you,\nAlex.",
            style = MaterialTheme.typography.displayMedium,
            color = extras.onHero,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Your business accounts are ready when you are.",
            style = MaterialTheme.typography.bodyLarge,
            color = extras.heroMuted,
        )
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun PinDots(length: Int, error: Boolean) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .testTag("pin_dots"),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(MAX_PIN_LENGTH) { index ->
            val filled = index < length
            Box(
                modifier = Modifier
                    .size(15.dp)
                    .clip(CircleShape)
                    .background(
                        if (filled) {
                            if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        },
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (error) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        shape = CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun PinPad(
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "delete"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { key ->
                    if (key.isEmpty()) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        PinKey(
                            key = key,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (key == "delete") onDelete() else onDigit(key)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PinKey(
    key: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(50.dp)
            .testTag(if (key == "delete") "pin_key_delete" else "pin_key_$key"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (key == "delete") {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Backspace,
                    contentDescription = "Delete digit",
                    modifier = Modifier.size(22.dp),
                )
            } else {
                Text(
                    text = key,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}
