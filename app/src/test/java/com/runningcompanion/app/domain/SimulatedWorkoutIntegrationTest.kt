package com.runningcompanion.app.domain

import com.google.common.truth.Truth.assertThat
import com.runningcompanion.app.domain.engine.CueEvent
import com.runningcompanion.app.domain.engine.RunnerState
import com.runningcompanion.app.domain.engine.WorkoutRunner
import com.runningcompanion.app.domain.model.SegmentType
import com.runningcompanion.app.domain.model.WorkoutTemplates
import com.runningcompanion.app.simulation.MockLocationProvider
import org.junit.Test

class SimulatedWorkoutIntegrationTest {

    @Test
    fun testNorwegian4x4_fullSimulationWithMockGps() {
        val workout = WorkoutTemplates.norwegian4x4
        val runner = WorkoutRunner(workout)
        val mockGps = MockLocationProvider(basePaceSecPerKm = 300.0) // 5:00 min/km

        val collectedCues = mutableListOf<CueEvent>()
        val startCues = runner.start(mockGps.currentTimestamp)
        collectedCues.addAll(startCues)

        var currentSegmentIdx = 0

        // Simulate second-by-second execution
        while (runner.activeState == RunnerState.RUNNING) {
            val point = mockGps.nextPoint(intervalMs = 1000L)
            val locCues = runner.onLocation(point, point.timestamp)
            collectedCues.addAll(locCues)

            val tickCues = runner.tick(point.timestamp)
            collectedCues.addAll(tickCues)

            val snapshot = runner.getSnapshot(point.timestamp)
            if (snapshot.currentSegmentIndex != currentSegmentIdx) {
                currentSegmentIdx = snapshot.currentSegmentIndex
            }
        }

        // Runner must have finished successfully
        assertThat(runner.activeState).isEqualTo(RunnerState.FINISHED)

        // Verify cues
        val segmentStartCues = collectedCues.filterIsInstance<CueEvent.SegmentStart>()
        // 1 warmup + 4 runs + 4 rests + 1 cooldown = 10 segments
        assertThat(segmentStartCues).hasSize(10)

        val countdownCues = collectedCues.filterIsInstance<CueEvent.Countdown>()
        assertThat(countdownCues).isNotEmpty()

        val setsLeftCues = collectedCues.filterIsInstance<CueEvent.SetsLeft>()
        // 4 rests -> sets left 3, 2, 1, 0 (0 is not emitted)
        assertThat(setsLeftCues).hasSize(3)

        val completeCues = collectedCues.filterIsInstance<CueEvent.WorkoutComplete>()
        assertThat(completeCues).hasSize(1)

        val kmSplitCues = collectedCues.filterIsInstance<CueEvent.KmSplitAlert>()
        // Total distance > 8.0 km, so we should have alerts for km 1 through 8
        assertThat(kmSplitCues).hasSize(8)
        assertThat(kmSplitCues.map { it.kilometer }).containsExactly(1, 2, 3, 4, 5, 6, 7, 8).inOrder()

        // Verify final session results
        val session = runner.buildSessionResult(mockGps.currentTimestamp)
        assertThat(session.results).hasSize(10)

        // Warmup: 600s at 5:00 min/km (300 s/km) = 2.0 km -> should have at least 1-2 km splits
        val warmupResult = session.results.first()
        assertThat(warmupResult.type).isEqualTo(SegmentType.WARMUP)
        assertThat(warmupResult.durationSec).isEqualTo(600L)
        assertThat(warmupResult.distanceM).isGreaterThan(1900.0)
        assertThat(warmupResult.kmSplits).isNotEmpty()

        // Total workout duration = 10m + 4*(4m + 3m) + 5m = 43 minutes = 2580 seconds
        assertThat(session.totals.durationSec).isEqualTo(2580L)
        assertThat(session.totals.distanceM).isGreaterThan(8000.0) // ~8.6 km total
    }
}
