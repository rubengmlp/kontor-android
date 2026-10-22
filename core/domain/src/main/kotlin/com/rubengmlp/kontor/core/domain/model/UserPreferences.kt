package com.rubengmlp.kontor.core.domain.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val biometricEnabled: Boolean = true,
    val hideBalances: Boolean = false,
)
