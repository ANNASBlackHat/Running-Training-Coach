package com.runningcompanion.app.domain

import com.google.common.truth.Truth.assertThat
import com.runningcompanion.app.domain.model.PaceRange
import com.runningcompanion.app.domain.model.Segment
import com.runningcompanion.app.domain.model.SegmentLength
import com.runningcompanion.app.domain.model.SegmentResult
import com.runningcompanion.app.domain.model.SegmentType
import com.runningcompanion.app.domain.model.Session
import com.runningcompanion.app.domain.model.SessionTotals
import com.runningcompanion.app.domain.model.Workout
import com.runningcompanion.app.domain.model.WorkoutItem
import com.runningcompanion.app.domain.share.ShareCardModel
import org.junit.Test

class ShareCardModelTest {

    private fun buildWorkout(): Workout {
        return Workout(
            id = "w1",
            name = "Intervals",
            isTemplate = false,
            items = listOf(
                WorkoutItem.Single(Segment("s1", SegmentType.WARMUP, SegmentLength.Time(600))),
                WorkoutItem.Repeat(
                    count = 3,
                    segments = listOf(
                        Segment("s2", SegmentType.RUN, SegmentLength.Distance(400), PaceRange(240, 270)),
                        Segment("s3", SegmentType.REST, SegmentLength.Time(90))
                    )
                )
            )
        )
    }

    private fun segmentResult(
        index: Int,
        type: SegmentType,
        setNumber: Int? = null,
        pace: Double? = null,
        durationSec: Long = 60,
        distanceM: Double = 400.0,
        elevationGainM: Double? = null
    ): SegmentResult {
        return SegmentResult(
            index = index,
            type = type,
            setNumber = setNumber,
            startedAt = 0L,
            endedAt = durationSec * 1000,
            durationSec = durationSec,
            distanceM = distanceM,
            avgPaceSecPerKm = pace,
            elevationGainM = elevationGainM
        )
    }

    private fun buildSession(paces: List<Double?>, elevation: Boolean = false): Session {
        val workout = buildWorkout()
        val results = mutableListOf(
            segmentResult(0, SegmentType.WARMUP, pace = 330.0, durationSec = 600, distanceM = 3000.0)
        )
        for (set in 1..3) {
            val pace = paces.getOrElse(set - 1) { null }
            results.add(segmentResult(set * 2 - 1, SegmentType.RUN, setNumber = set, pace = pace, distanceM = 400.0))
            results.add(
                segmentResult(
                    set * 2,
                    SegmentType.REST,
                    setNumber = set,
                    pace = 360.0,
                    durationSec = 90,
                    distanceM = 300.0
                )
            )
        }
        val finalResults = if (elevation) {
            results.mapIndexed { i, res ->
                if (i == 0) res.copy(elevationGainM = 100.0) else res
            }
        } else {
            results
        }
        val totalDistance = finalResults.sumOf { it.distanceM }
        return Session(
            id = "sess1",
            workoutId = "w1",
            workoutSnapshot = workout,
            startedAt = 1_700_000_000_000L,
            endedAt = 1_700_000_990_000L,
            results = finalResults,
            totals = SessionTotals(2070L, totalDistance, 350.0),
            track = emptyList()
        )
    }

    @Test
    fun barsContainOnlyRunSegmentsWithPace() {
        val model = ShareCardModel.fromSession(buildSession(listOf(250.0, 255.0, 260.0)))
        assertThat(model.bars).hasSize(3)
        assertThat(model.bars.map { it.label }).containsExactly("S1", "S2", "S3").inOrder()
        assertThat(model.bars[0].paceSecPerKm).isEqualTo(250.0)
        assertThat(model.bars[0].targetFastSecPerKm).isEqualTo(240.0)
        assertThat(model.bars[0].targetSlowSecPerKm).isEqualTo(270.0)
    }

    @Test
    fun barsFlagOutOfRangePace() {
        val model = ShareCardModel.fromSession(buildSession(listOf(280.0, 255.0, 235.0)))
        assertThat(model.bars[0].isOutOfRange).isTrue()
        assertThat(model.bars[1].isOutOfRange).isFalse()
        assertThat(model.bars[2].isOutOfRange).isTrue()
    }

    @Test
    fun missingPaceIsExcludedFromBars() {
        val model = ShareCardModel.fromSession(buildSession(listOf(null, 255.0, 260.0)))
        assertThat(model.bars).hasSize(2)
    }

    @Test
    fun consistencyComputesFastestSlowestAndCv() {
        val model = ShareCardModel.fromSession(buildSession(listOf(240.0, 270.0, 255.0)))
        val c = model.consistency
        assertThat(c).isNotNull()
        assertThat(c!!.fastestSecPerKm).isEqualTo(240.0)
        assertThat(c.slowestSecPerKm).isEqualTo(270.0)
        // population stdev of [240, 270, 255] = sqrt((225+225+0)/3) ≈ 12.247, mean 255
        assertThat(c.cvPercent).isWithin(0.1).of(12.2474487 / 255.0 * 100.0)
        assertThat(model.fastestSetPaceSecPerKm).isEqualTo(240.0)
    }

    @Test
    fun singleSetHasNoConsistency() {
        val model = ShareCardModel.fromSession(buildSession(listOf(250.0, null, null)))
        assertThat(model.consistency).isNull()
    }

    @Test
    fun lapsTruncateToCapWithCount() {
        val workout = buildWorkout()
        val results = (0 until 20).map { i ->
            segmentResult(i, if (i % 2 == 0) SegmentType.RUN else SegmentType.REST)
        }
        val session = Session(
            id = "sess2",
            workoutId = "w1",
            workoutSnapshot = workout,
            startedAt = 0L,
            endedAt = 0L,
            results = results,
            totals = SessionTotals(0L, 0.0, null),
            track = emptyList()
        )
        val model = ShareCardModel.fromSession(session)
        assertThat(model.laps).hasSize(ShareCardModel.MAX_VISIBLE_LAPS)
        assertThat(model.lapTruncationCount).isEqualTo(20 - ShareCardModel.MAX_VISIBLE_LAPS)
    }

    @Test
    fun lapSetLabelUsesSetTotalFromWorkout() {
        val model = ShareCardModel.fromSession(buildSession(listOf(250.0, 255.0, 260.0)))
        val runLap = model.laps.first { it.type == SegmentType.RUN }
        assertThat(runLap.setLabel).isEqualTo("1/3")
    }

    @Test
    fun elevationGainSumsAcrossSegments() {
        val model = ShareCardModel.fromSession(buildSession(listOf(250.0, 255.0, 260.0), elevation = true))
        assertThat(model.elevationGainM).isEqualTo(100.0)
        val noElevation = ShareCardModel.fromSession(buildSession(listOf(250.0, 255.0, 260.0)))
        assertThat(noElevation.elevationGainM).isNull()
    }

    @Test
    fun intervalTagIsBuiltFromWorkout() {
        val model = ShareCardModel.fromSession(buildSession(listOf(250.0, 255.0, 260.0)))
        assertThat(model.intervalTag).contains("3 × 400 m")
    }

    @Test
    fun trackFlagReflectsSession() {
        val session = buildSession(listOf(250.0, 255.0, 260.0))
        assertThat(ShareCardModel.fromSession(session).hasTrack).isFalse()
        val withTrack = session.copy(
            track = listOf(
                com.runningcompanion.app.domain.model.GpsPoint(1L, 52.0, 13.0, 1f),
                com.runningcompanion.app.domain.model.GpsPoint(2L, 52.001, 13.001, 1f)
            )
        )
        assertThat(ShareCardModel.fromSession(withTrack).hasTrack).isTrue()
    }
}
