package com.runningcompanion.app.ui.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.runningcompanion.app.domain.model.PaceRange
import com.runningcompanion.app.domain.model.Segment
import com.runningcompanion.app.domain.model.SegmentLength
import com.runningcompanion.app.domain.model.SegmentType
import com.runningcompanion.app.domain.model.Session
import com.runningcompanion.app.domain.model.SessionTotals
import com.runningcompanion.app.domain.model.Workout
import com.runningcompanion.app.domain.model.WorkoutItem
import com.runningcompanion.app.domain.model.WorkoutTemplates
import com.runningcompanion.app.ui.theme.RunningTheme

private val previewCustomWorkout = Workout(
    id = "preview_custom",
    name = "Lunch Tempo",
    items = listOf(
        WorkoutItem.Single(Segment("p1", SegmentType.WARMUP, SegmentLength.Time(600))),
        WorkoutItem.Single(
            Segment(
                "p2",
                SegmentType.RUN,
                SegmentLength.Time(1200),
                PaceRange(fastSecPerKm = 300, slowSecPerKm = 315)
            )
        ),
        WorkoutItem.Single(Segment("p3", SegmentType.COOLDOWN, SegmentLength.Time(300)))
    )
)

private val previewSession = Session(
    id = "preview_session",
    workoutId = previewCustomWorkout.id,
    workoutSnapshot = previewCustomWorkout,
    startedAt = System.currentTimeMillis(),
    endedAt = System.currentTimeMillis(),
    results = emptyList(),
    totals = SessionTotals(durationSec = 2100, distanceM = 6350.0, avgPaceSecPerKm = 330.0),
    track = emptyList()
)

@Composable
private fun PreviewHome(
    workouts: List<Workout>,
    recentSession: Session? = null,
    activeWorkout: Workout? = null
) {
    RunningTheme {
        HomeScreen(
            workouts = workouts,
            recentSession = recentSession,
            activeWorkout = activeWorkout,
            onStartWorkout = {},
            onResumeWorkout = {},
            onWorkoutDetail = {},
            onNewWorkout = {},
            onHistoryClicked = {},
            onSessionClicked = {},
            onDuplicateWorkout = {},
            onRenameWorkout = { _, _ -> },
            onDeleteWorkout = {}
        )
    }
}

@Preview(name = "Populated", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun HomePopulatedPreview() {
    PreviewHome(
        workouts = WorkoutTemplates.allTemplates + previewCustomWorkout,
        recentSession = previewSession
    )
}

@Preview(name = "Empty my workouts", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun HomeEmptyPreview() {
    PreviewHome(workouts = WorkoutTemplates.allTemplates)
}

@Preview(name = "Workout in progress", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun HomeInProgressPreview() {
    PreviewHome(
        workouts = WorkoutTemplates.allTemplates + previewCustomWorkout,
        activeWorkout = previewCustomWorkout
    )
}
