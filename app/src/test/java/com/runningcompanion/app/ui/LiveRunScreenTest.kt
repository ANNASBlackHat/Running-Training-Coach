package com.runningcompanion.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.runningcompanion.app.domain.model.WorkoutTemplates
import com.runningcompanion.app.ui.screens.run.LiveRunScreen
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
class LiveRunScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun testMinimizeLeavesTheScreenWithoutStoppingTheRun() {
        var minimized = false
        var stopped = false

        composeRule.setContent {
            RunningTheme {
                LiveRunScreen(
                    snapshot = null,
                    allSegments = WorkoutTemplates.repeats400m.flatten(),
                    onPauseClicked = {},
                    onResumeClicked = {},
                    onSkipClicked = {},
                    onStopConfirmed = { stopped = true },
                    onMinimize = { minimized = true }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Back to workouts").performClick()

        assertThat(minimized).isTrue()
        assertThat(stopped).isFalse()
    }
}
