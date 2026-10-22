package com.rubengmlp.kontor

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rubengmlp.kontor.core.designsystem.theme.KontorTheme
import com.rubengmlp.kontor.core.domain.model.ThemeMode
import com.rubengmlp.kontor.ui.UnlockScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        if (!BuildConfig.DEBUG) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }

        setContent {
            val preferences by appViewModel.preferences.collectAsStateWithLifecycle()
            val darkTheme = when (preferences.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            KontorTheme(darkTheme = darkTheme) {
                val unlocked by appViewModel.unlocked.collectAsStateWithLifecycle()
                val view = LocalView.current
                SideEffect {
                    val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
                    WindowCompat.getInsetsController(window, view).apply {
                        // The unlock screen always sits on the dark green hero.
                        isAppearanceLightStatusBars = unlocked && !darkTheme
                        isAppearanceLightNavigationBars = !darkTheme
                    }
                }
                if (unlocked) {
                    KontorApp(onLock = appViewModel::lock)
                } else {
                    UnlockScreen(
                        biometricEnabled = preferences.biometricEnabled,
                        onUnlocked = appViewModel::unlock,
                    )
                }
            }
        }
    }
}
