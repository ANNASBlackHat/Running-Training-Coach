package com.runningcompanion.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.runningcompanion.app.domain.model.SegmentResult
import com.runningcompanion.app.domain.model.SegmentType
import com.runningcompanion.app.domain.model.Session
import com.runningcompanion.app.domain.model.SessionTotals
import com.runningcompanion.app.domain.model.WorkoutTemplates
import com.runningcompanion.app.ui.screens.results.ResultsScreen
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
class ResultsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun buildSession(): Session {
        val workout = WorkoutTemplates.repeats400m
        val results = listOf(
            SegmentResult(
                index = 0,
                type = SegmentType.WARMUP,
                setNumber = null,
                startedAt = 0L,
                endedAt = 600_000L,
                durationSec = 600L,
                distanceM = 3000.0,
                avgPaceSecPerKm = 330.0
            ),
            SegmentResult(
                index = 1,
                type = SegmentType.RUN,
                setNumber = 1,
                startedAt = 600_000L,
                endedAt = 780_000L,
                durationSec = 180L,
                distanceM = 400.0,
                avgPaceSecPerKm = 250.0
            )
        )
        return Session(
            id = "sess-ui",
            workoutId = workout.id,
            workoutSnapshot = workout,
            startedAt = 1_700_000_000_000L,
            endedAt = 1_700_000_600_000L,
            results = results,
            totals = SessionTotals(780L, 3400.0, 318.0),
            track = emptyList()
        )
    }

    @Test
    fun shareIconIsDisplayedAndOpensSheet() {
        val session = buildSession()
        composeRule.setContent {
            RunningTheme {
                ResultsScreen(session = session, onHomeClicked = {})
            }
        }

        composeRule.onNodeWithContentDescription("Share results").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Share results").performClick()

        composeRule.onNodeWithText("Share workout").assertIsDisplayed()
        composeRule.onNodeWithText("Share").assertIsDisplayed()
        composeRule.onNodeWithText("Save").assertIsDisplayed()
        composeRule.onNodeWithText("GPX").assertIsDisplayed()
        composeRule.onNodeWithText("Done").assertIsDisplayed()

        composeRule.onNodeWithText("Done").performClick()
        composeRule.onNodeWithText("Share workout").assertDoesNotExist()
    }
}
