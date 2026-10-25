package com.runningcompanion.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.runningcompanion.app.domain.model.PaceRange
import com.runningcompanion.app.domain.model.Segment
import com.runningcompanion.app.domain.model.SegmentLength
import com.runningcompanion.app.domain.model.SegmentType
import com.runningcompanion.app.domain.model.Session
import com.runningcompanion.app.domain.model.SessionTotals
import com.runningcompanion.app.domain.model.Workout
import com.runningcompanion.app.domain.model.WorkoutItem
import com.runningcompanion.app.domain.model.WorkoutTemplates
import com.runningcompanion.app.ui.screens.home.HomeScreen
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
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val customWorkout = Workout(
        id = "custom_1",
        name = "Morning Hills",
        isTemplate = false,
        items = listOf(
            WorkoutItem.Single(
                Segment("s1", SegmentType.WARMUP, SegmentLength.Time(600))
            ),
            WorkoutItem.Single(
                Segment("s2", SegmentType.RUN, SegmentLength.Time(1200), PaceRange(300, 320))
            )
        )
    )

    private val template = WorkoutTemplates.repeats400m

    private fun setHome(
        workouts: List<Workout> = listOf(customWorkout),
        recentSession: Session? = null,
        activeWorkout: Workout? = null,
        onStart: (Workout) -> Unit = {},
        onResume: () -> Unit = {},
        onDetail: (Workout) -> Unit = {},
        onNewWorkout: () -> Unit = {},
        onHistory: () -> Unit = {},
        onSession: (Session) -> Unit = {},
        onDuplicate: (Workout) -> Unit = {},
        onRename: (Workout, String) -> Unit = { _, _ -> },
        onDelete: (Workout) -> Unit = {}
    ) {
        composeRule.setContent {
            RunningTheme {
                HomeScreen(
                    workouts = workouts,
                    recentSession = recentSession,
                    activeWorkout = activeWorkout,
                    onStartWorkout = onStart,
                    onResumeWorkout = onResume,
                    onWorkoutDetail = onDetail,
                    onNewWorkout = onNewWorkout,
                    onHistoryClicked = onHistory,
                    onSessionClicked = onSession,
                    onDuplicateWorkout = onDuplicate,
                    onRenameWorkout = onRename,
                    onDeleteWorkout = onDelete
                )
            }
        }
    }

    private fun buildSession(): Session = Session(
        id = "session_1",
        workoutId = customWorkout.id,
        workoutSnapshot = customWorkout,
        startedAt = System.currentTimeMillis(),
        endedAt = System.currentTimeMillis(),
        results = emptyList(),
        totals = SessionTotals(durationSec = 1800, distanceM = 5000.0, avgPaceSecPerKm = 360.0),
        track = emptyList()
    )

    @Test
    fun testWorkoutRowShowsTruthfulStructureAndDuration() {
        setHome()

        composeRule.onNodeWithText("2 segments · ~30 min", substring = true).assertIsDisplayed()
    }

    @Test
    fun testTemplateRailShowsBadgeAndIntervalSummary() {
        setHome(workouts = listOf(template))

        composeRule.onNodeWithText("TEMPLATE").assertIsDisplayed()
        composeRule.onNodeWithText("8 × 400 m", substring = true).assertExists()
        composeRule.onNodeWithContentDescription("Start 400m Repeats")
            .assertExists()
    }

    @Test
    fun testTemplatesHaveNoRowMenu() {
        setHome(workouts = listOf(template))

        composeRule.onNodeWithContentDescription("More options for ${template.name}")
            .assertDoesNotExist()
    }

    @Test
    fun testRowMenuOffersRenameDuplicateAndDelete() {
        setHome()

        composeRule.onNodeWithContentDescription("More options for Morning Hills").performClick()

        composeRule.onNodeWithText("Rename").assertExists()
        composeRule.onNodeWithText("Duplicate").assertExists()
        composeRule.onNodeWithText("Delete").assertExists()
    }

    @Test
    fun testDeleteRequiresConfirmation() {
        var deleted: Workout? = null
        setHome(onDelete = { deleted = it })

        composeRule.onNodeWithContentDescription("More options for Morning Hills").performClick()
        composeRule.onNodeWithText("Delete").performClick()

        assertThat(deleted).isNull()
        composeRule.onNodeWithText("Delete workout?").assertExists()

        composeRule.onNodeWithTag("confirm_delete").performClick()

        assertThat(deleted?.id).isEqualTo(customWorkout.id)
    }

    @Test
    fun testCancellingDeleteKeepsTheWorkout() {
        var deleted: Workout? = null
        setHome(onDelete = { deleted = it })

        composeRule.onNodeWithContentDescription("More options for Morning Hills").performClick()
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.onNodeWithText("Cancel").performClick()

        assertThat(deleted).isNull()
        composeRule.onNodeWithText("Delete workout?").assertDoesNotExist()
    }

    @Test
    fun testDuplicateEmitsTheWorkout() {
        var duplicated: Workout? = null
        setHome(onDuplicate = { duplicated = it })

        composeRule.onNodeWithContentDescription("More options for Morning Hills").performClick()
        composeRule.onNodeWithText("Duplicate").performClick()

        assertThat(duplicated?.id).isEqualTo(customWorkout.id)
    }

    // The rename dialog itself cannot be opened in this environment: a dialog window
    // containing a text field never reaches idle under Robolectric. The form is covered
    // by RenameWorkoutFormTest instead.

    @Test
    fun testLastRunCardHiddenWithoutSessions() {
        setHome(recentSession = null)

        composeRule.onNodeWithTag("recent_session_card").assertDoesNotExist()
    }

    @Test
    fun testLastRunCardShowsTheLatestRun() {
        setHome(recentSession = buildSession())

        composeRule.onNodeWithTag("recent_session_card").assertExists()
        composeRule.onNodeWithText("5.00 km", substring = true).assertExists()
        composeRule.onNodeWithText("6:00", substring = true).assertExists()
    }

    @Test
    fun testLastRunCardOpensTheSession() {
        val session = buildSession()
        var opened: Session? = null
        setHome(recentSession = session, onSession = { opened = it })

        composeRule.onNodeWithTag("recent_session_card").performClick()

        assertThat(opened?.id).isEqualTo(session.id)
    }

    @Test
    fun testResumeBarHiddenWhenNoWorkoutIsActive() {
        setHome(activeWorkout = null)

        composeRule.onNodeWithTag("resume_run_bar").assertDoesNotExist()
    }

    @Test
    fun testResumeBarShowsActiveWorkoutAndResumes() {
        var resumed = false
        setHome(activeWorkout = customWorkout, onResume = { resumed = true })

        composeRule.onNodeWithTag("resume_run_bar").assertIsDisplayed()
        composeRule.onNodeWithText("WORKOUT IN PROGRESS").assertExists()

        composeRule.onNodeWithTag("resume_run_bar").performClick()

        assertThat(resumed).isTrue()
    }

    @Test
    fun testEmptyStateShownWhenThereAreNoCustomWorkouts() {
        setHome(workouts = listOf(template))

        composeRule.onNodeWithText("No custom workouts yet").assertExists()
    }

    @Test
    fun testEmptyStateHiddenWhenCustomWorkoutsExist() {
        setHome()

        composeRule.onNodeWithText("No custom workouts yet").assertDoesNotExist()
    }

    @Test
    fun testStartButtonEmitsTheWorkout() {
        var started: Workout? = null
        setHome(onStart = { started = it })

        composeRule.onNodeWithContentDescription("Start Morning Hills").performClick()

        assertThat(started?.id).isEqualTo(customWorkout.id)
    }

    @Test
    fun testRowTapOpensDetail() {
        var opened: Workout? = null
        setHome(onDetail = { opened = it })

        composeRule.onNodeWithText("Morning Hills").performClick()

        assertThat(opened?.id).isEqualTo(customWorkout.id)
    }

    @Test
    fun testNewWorkoutActionIsAvailable() {
        var created = 0
        setHome(onNewWorkout = { created++ })

        composeRule.onNodeWithText("New workout").performClick()

        assertThat(created).isEqualTo(1)
    }

    @Test
    fun testHistoryActionIsAvailable() {
        var historyOpened = 0
        setHome(onHistory = { historyOpened++ })

        composeRule.onNodeWithText("History").performClick()

        assertThat(historyOpened).isEqualTo(1)
    }
}
