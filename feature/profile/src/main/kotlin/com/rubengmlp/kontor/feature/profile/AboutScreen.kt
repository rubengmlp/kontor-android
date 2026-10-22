package com.rubengmlp.kontor.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rubengmlp.kontor.core.designsystem.components.KontorCard
import com.rubengmlp.kontor.core.designsystem.components.KontorScreenBackground
import com.rubengmlp.kontor.core.designsystem.components.KontorTopBar
import com.rubengmlp.kontor.core.designsystem.theme.KontorTheme

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KontorScreenBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            KontorTopBar(
                title = "How this app is built",
                subtitle = "Architecture, decisions and demo boundaries",
                onBack = onBack,
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Kontor is a small business-banking app built to be read, tested and explained — not just demoed.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                KontorCard {
                    SectionTitle("Layers")
                    Bullet("UI", "Jetpack Compose, one immutable UiState per screen, state hoisting.")
                    Bullet("Presentation", "ViewModel + StateFlow, events go up, state goes down.")
                    Bullet("Domain", "Pure Kotlin models, validation and repository contracts.")
                    Bullet("Data", "Room as source of truth, Retrofit for the API, DataStore for preferences.")
                    Bullet("DI", "Hilt with compile-time-checked graphs and standard Android scopes.")
                }
                KontorCard {
                    SectionTitle("Module graph")
                    Text(
                        text = """
                            :app
                             ├── :feature:accounts
                             ├── :feature:transfer
                             └── :feature:profile
                                    │
                             :core:designsystem
                             :core:domain  ←  :core:data
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                KontorCard {
                    SectionTitle("Decisions worth defending")
                    Bullet("Money", "Long cents everywhere. Never Double for balances.")
                    Bullet("Offline first", "Screens observe Room; refresh writes through the repository.")
                    Bullet("Unidirectional flow", "No mutable state shared with the UI. Compose only renders.")
                    Bullet("Atomic transfers", "Balance update and ledger entry run in one Room transaction.")
                    Bullet("Demo API", "Retrofit + OkHttp are real; one interceptor serves bundled JSON.")
                    Bullet("Navigation", "Type-safe routes with @Serializable, IDs instead of objects.")
                }
                KontorCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(Modifier.width(10.dp))
                        SectionTitle("Security boundaries", onPrimaryContainer = true)
                    }
                    Text(
                        text = "This is a demo build. The unlock screen uses a real BiometricPrompt when the device supports it, with PIN 1234 as a fallback. A production banking app would authenticate against a backend, store short-lived tokens encrypted with an Android Keystore key, bind sensitive actions to a CryptoObject, pin certificates, and apply FLAG_SECURE, R8 and Play Integrity.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                KontorCard {
                    SectionTitle("What I'd build next")
                    Bullet("Auth", "OIDC + refresh tokens, Keystore-backed storage and biometric step-up.")
                    Bullet("Scale", "Paging 3 for activity, idempotency keys and server-confirmed balances.")
                    Bullet("Quality", "Screenshot tests, Turbine coverage for every ViewModel, accessibility audit.")
                    Bullet("Delivery", "Modularized features, convention plugins, release trains and Play staged rollout.")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Code,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "github.com/rubengmlp · built with Kotlin & Compose",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, onPrimaryContainer: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = if (onPrimaryContainer) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun Bullet(title: String, body: String) {
    Row(
        modifier = Modifier.padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .background(KontorTheme.colors.positive, CircleShape),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}
