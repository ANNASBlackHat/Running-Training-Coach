package com.runningcompanion.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.runningcompanion.app.ui.screens.home.RenameWorkoutForm
import com.runningcompanion.app.ui.theme.RunningTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RenameWorkoutFormTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setForm(
        initialName: String = "Morning Hills",
        onCancel: () -> Unit = {},
        onConfirm: (String) -> Unit = {}
    ) {
        composeRule.setContent {
            RunningTheme {
                RenameWorkoutForm(
                    initialName = initialName,
                    onCancel = onCancel,
                    onConfirm = onConfirm,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }

    @Test
    fun testSavingEmitsTheTrimmedName() {
        var saved: String? = null
        setForm(onConfirm = { saved = it })

        // The blinking cursor never lets Robolectric reach idle, so drive the clock.
        composeRule.mainClock.autoAdvance = false
        composeRule.onNode(hasSetTextAction()).performTextReplacement("  Evening Hills  ")
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithTag("confirm_rename").performClick()
        composeRule.mainClock.advanceTimeByFrame()

        assertThat(saved).isEqualTo("Evening Hills")
    }

    @Test
    fun testSaveIsDisabledForABlankName() {
        var saved: String? = null
        setForm(onConfirm = { saved = it })

        composeRule.mainClock.autoAdvance = false
        composeRule.onNode(hasSetTextAction()).performTextClearance()
        composeRule.mainClock.advanceTimeByFrame()

        composeRule.onNodeWithTag("confirm_rename").assertIsNotEnabled()
        composeRule.onNodeWithTag("confirm_rename").performClick()

        assertThat(saved).isNull()
    }

    @Test
    fun testCancelDoesNotSave() {
        var saved: String? = null
        var cancelled = false
        setForm(onCancel = { cancelled = true }, onConfirm = { saved = it })

        composeRule.onNodeWithTag("confirm_rename").assertExists()
        composeRule.onNodeWithText("Cancel").performClick()

        assertThat(cancelled).isTrue()
        assertThat(saved).isNull()
    }
}
