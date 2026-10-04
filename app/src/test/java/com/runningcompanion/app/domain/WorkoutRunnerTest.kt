package com.runningcompanion.app.domain

import com.google.common.truth.Truth.assertThat
import com.runningcompanion.app.domain.engine.CueEvent
import com.runningcompanion.app.domain.engine.RunnerState
import com.runningcompanion.app.domain.engine.WorkoutRunner
import com.runningcompanion.app.domain.model.GpsPoint
import com.runningcompanion.app.domain.model.Segment
import com.runningcompanion.app.domain.model.SegmentLength
import com.runningcompanion.app.domain.model.SegmentType
import com.runningcompanion.app.domain.model.Workout
import com.runningcompanion.app.domain.model.WorkoutItem
import org.junit.Test

class WorkoutRunnerTest {

    private val sampleWorkout = Workout(
        id = "w1",
        name = "Test 2x(1m run / 30s rest)",
        isTemplate = false,
        items = listOf(
            WorkoutItem.Repeat(
                count = 2,
                segments = listOf(
                    Segment(
                        id = "run",
                        type = SegmentType.RUN,
                        length = SegmentLength.Time(60) // 60s
                    ),
                    Segment(
                        id = "rest",
                        type = SegmentType.REST,
                        length = SegmentLength.Time(30) // 30s
                    )
                )
            )
        )
    )

    @Test
    fun testWorkoutRunner_fullExecutionLifecycle() {
        val runner = WorkoutRunner(sampleWorkout)
        assertThat(runner.activeState).isEqualTo(RunnerState.IDLE)

        var t = 1_000_000L
        val startCues = runner.start(t)
        assertThat(runner.activeState).isEqualTo(RunnerState.RUNNING)
        assertThat(startCues.any { it is CueEvent.SegmentStart }).isTrue()

        // Tick 60 seconds to finish run segment 1
        t += 60_000L
        val seg1FinishCues = runner.tick(t)
        // Should transition to Rest segment (index 1)
        val snap1 = runner.getSnapshot(t)
        assertThat(snap1.currentSegmentIndex).isEqualTo(1)
        assertThat(snap1.currentSegment?.segment?.type).isEqualTo(SegmentType.REST)
        assertThat(seg1FinishCues.any { it is CueEvent.SegmentStart }).isTrue()

        // Tick 30 seconds to finish rest segment 1
        t += 30_000L
        val rest1FinishCues = runner.tick(t)
        // Should announce sets left: "1 sets left"
        assertThat(rest1FinishCues.any { it is CueEvent.SetsLeft }).isTrue()
        val snap2 = runner.getSnapshot(t)
        assertThat(snap2.currentSegmentIndex).isEqualTo(2)
        assertThat(snap2.currentSegment?.setNumber).isEqualTo(2)

        // Tick 60 seconds (run 2)
        t += 60_000L
        runner.tick(t)

        // Tick 30 seconds (rest 2 - final segment)
        t += 30_000L
        val finalCues = runner.tick(t)
        assertThat(runner.activeState).isEqualTo(RunnerState.FINISHED)
        assertThat(finalCues.any { it is CueEvent.WorkoutComplete }).isTrue()

        val session = runner.buildSessionResult(t)
        assertThat(session.results).hasSize(4)
        assertThat(session.totals.durationSec).isEqualTo(180L)
    }

    @Test
    fun testWorkoutRunner_pauseAndResume() {
        val runner = WorkoutRunner(sampleWorkout)
        var t = 10_000L
        runner.start(t)

        // Advance 20 seconds
        t += 20_000L
        runner.tick(t)
        var snap = runner.getSnapshot(t)
        assertThat(snap.segmentElapsedSec).isEqualTo(20L)

        // Pause for 15 seconds
        runner.pause(t)
        assertThat(runner.activeState).isEqualTo(RunnerState.PAUSED)
        t += 15_000L
        runner.tick(t)
        // Elapsed time should NOT increase while paused
        snap = runner.getSnapshot(t)
        assertThat(snap.segmentElapsedSec).isEqualTo(20L)

        // Resume and advance 10 seconds
        runner.resume(t)
        assertThat(runner.activeState).isEqualTo(RunnerState.RUNNING)
        t += 10_000L
        runner.tick(t)
        snap = runner.getSnapshot(t)
        assertThat(snap.segmentElapsedSec).isEqualTo(30L)
    }

    @Test
    fun testWorkoutRunner_distanceSegmentTransition() {
        val distanceWorkout = Workout(
            id = "w2",
            name = "Distance Test",
            items = listOf(
                WorkoutItem.Single(
                    Segment(
                        id = "d1",
                        type = SegmentType.RUN,
                        length = SegmentLength.Distance(100) // 100m
                    )
                ),
                WorkoutItem.Single(
                    Segment(
                        id = "d2",
                        type = SegmentType.COOLDOWN,
                        length = SegmentLength.Time(60)
                    )
                )
            )
        )

        val runner = WorkoutRunner(distanceWorkout)
        var t = 10_000L
        runner.start(t)

        // Feed GPS points accumulating 100 meters
        val p1 = GpsPoint(timestamp = t, latitude = 0.0, longitude = 0.0, accuracy = 5f)
        runner.onLocation(p1, t)

        // Point ~110m north (approx 0.001 deg lat is ~111m)
        t += 15_000L
        val p2 = GpsPoint(timestamp = t, latitude = 0.001, longitude = 0.0, accuracy = 5f)
        val cues = runner.onLocation(p2, t)

        // Distance threshold exceeded, should auto-advance to Cooldown segment
        val snap = runner.getSnapshot(t)
        assertThat(snap.currentSegmentIndex).isEqualTo(1)
        assertThat(snap.currentSegment?.segment?.type).isEqualTo(SegmentType.COOLDOWN)
        assertThat(cues.any { it is CueEvent.SegmentStart }).isTrue()
    }
}
