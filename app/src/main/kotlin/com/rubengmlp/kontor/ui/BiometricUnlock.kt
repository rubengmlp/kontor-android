package com.rubengmlp.kontor.ui

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

private const val ALLOWED_AUTHENTICATORS =
    BiometricManager.Authenticators.BIOMETRIC_WEAK or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL

class BiometricUnlock internal constructor(
    val isAvailable: Boolean,
    private val launch: () -> Unit,
) {
    fun authenticate() = launch()
}

/**
 * Wraps BiometricPrompt so the screen does not need to know about FragmentActivity or the
 * executor. This is a demo gate: the PIN fallback is intentionally simple.
 */
@Composable
fun rememberBiometricUnlock(
    enabled: Boolean,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
): BiometricUnlock {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val currentOnSuccess by rememberUpdatedState(onSuccess)
    val currentOnError by rememberUpdatedState(onError)

    val canAuthenticate = remember(enabled, context, activity) {
        enabled && activity != null &&
            BiometricManager.from(context)
                .canAuthenticate(ALLOWED_AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
    }

    val prompt = remember(activity, context) {
        activity?.let { fragmentActivity ->
            BiometricPrompt(
                fragmentActivity,
                ContextCompat.getMainExecutor(context),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(
                        result: BiometricPrompt.AuthenticationResult,
                    ) {
                        currentOnSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                            errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                            errorCode != BiometricPrompt.ERROR_CANCELED
                        ) {
                            currentOnError(errString.toString())
                        }
                    }
                },
            )
        }
    }

    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Kontor")
            .setSubtitle("Confirm it's you to view your business accounts")
            .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
            .setConfirmationRequired(false)
            .build()
    }

    return remember(canAuthenticate, prompt, promptInfo) {
        BiometricUnlock(isAvailable = canAuthenticate) {
            if (prompt != null) {
                prompt.authenticate(promptInfo)
            } else {
                currentOnError("Biometrics are not available on this device")
            }
        }
    }
}
