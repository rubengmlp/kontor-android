package com.rubengmlp.kontor.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rubengmlp.kontor.core.designsystem.theme.KontorTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UnlockScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setUnlockScreen(onUnlocked: () -> Unit) {
        composeRule.setContent {
            KontorTheme {
                UnlockScreen(
                    biometricEnabled = false,
                    onUnlocked = onUnlocked,
                )
            }
        }
    }

    @Test
    fun correctPinUnlocksTheApp() {
        var unlocked = false
        setUnlockScreen { unlocked = true }

        "1234".forEach { digit ->
            composeRule.onNodeWithTag("pin_key_$digit").performClick()
        }

        composeRule.waitUntil(timeoutMillis = 2_000) { unlocked }
        assertTrue(unlocked)
    }

    @Test
    fun wrongPinShowsAnErrorAndKeepsTheGateClosed() {
        var unlocked = false
        setUnlockScreen { unlocked = true }

        "0000".forEach { digit ->
            composeRule.onNodeWithTag("pin_key_$digit").performClick()
        }

        composeRule.waitUntil(timeoutMillis = 2_000) {
            composeRule.onAllNodesWithText("That PIN doesn't match. Try 1234.")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("That PIN doesn't match. Try 1234.")
            .assertIsDisplayed()
        assertFalse(unlocked)
    }
}
