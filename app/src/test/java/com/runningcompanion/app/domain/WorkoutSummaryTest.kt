package com.runningcompanion.app.domain

import com.google.common.truth.Truth.assertThat
import com.runningcompanion.app.domain.model.PaceRange
import com.runningcompanion.app.domain.model.RuntimeSegment
import com.runningcompanion.app.domain.model.Segment
import com.runningcompanion.app.domain.model.SegmentLength
import com.runningcompanion.app.domain.model.SegmentType
import com.runningcompanion.app.domain.model.Workout
import com.runningcompanion.app.domain.model.WorkoutItem
import com.runningcompanion.app.domain.model.WorkoutTemplates
import com.runningcompanion.app.domain.summary.WorkoutSummary
import org.junit.Test

class WorkoutSummaryTest {

    @Test
    fun testDistanceSegmentsUseTargetPaceMidpoint() {
        val interval = RuntimeSegment(
            index = 1,
            segment = Segment(
                id = "run",
                type = SegmentType.RUN,
                length = SegmentLength.Distance(400),
                paceRange = PaceRange(fastSecPerKm = 240, slowSecPerKm = 270) // midpoint 255
            )
        )

        // 400 m at 255 s/km = 102 s
        assertThat(WorkoutSummary.estimatedSeconds(interval)).isWithin(0.001).of(102.0)
    }

    @Test
    fun testDistanceSegmentsWithoutTargetFallBackToDefaultPace() {
        val longRun = RuntimeSegment(
            index = 0,
            segment = Segment(
                id = "run",
                type = SegmentType.RUN,
                length = SegmentLength.Distance(1000)
            )
        )

        assertThat(WorkoutSummary.estimatedSeconds(longRun))
            .isWithin(0.001)
            .of(WorkoutSummary.DEFAULT_PACE_SEC_PER_KM)
    }

    @Test
    fun testTimeSegmentsUseTheirDuration() {
        val warmup = RuntimeSegment(
            index = 0,
            segment = Segment(
                id = "warmup",
                type = SegmentType.WARMUP,
                length = SegmentLength.Time(600)
            )
        )

        assertThat(WorkoutSummary.estimatedSeconds(warmup)).isWithin(0.001).of(600.0)
    }

    @Test
    fun testEstimatedWorkoutDurationRespectsIntervalPaces() {
        // 10 min warmup + 8 × (400 m @ 4:15/km + 90 s rest) + 5 min cooldown
        assertThat(WorkoutSummary.estimatedSeconds(WorkoutTemplates.repeats400m)).isEqualTo(2436L)
    }

    @Test
    fun testIntervalSummaryDescribesDistanceRepeats() {
        assertThat(WorkoutSummary.intervalSummary(WorkoutTemplates.repeats400m))
            .isEqualTo("8 × 400 m")
    }

    @Test
    fun testIntervalSummaryDescribesTimeRepeats() {
        assertThat(WorkoutSummary.intervalSummary(WorkoutTemplates.norwegian4x4))
            .isEqualTo("4 × 4 min")
        assertThat(WorkoutSummary.intervalSummary(WorkoutTemplates.tempoSplits))
            .isEqualTo("3 × 8 min")
    }

    @Test
    fun testIntervalSummaryIsNullWithoutRepeats() {
        assertThat(WorkoutSummary.intervalSummary(WorkoutTemplates.tempoRun)).isNull()
    }

    @Test
    fun testIntervalSummaryJoinsMultipleRepeatBlocks() {
        val workout = Workout(
            id = "multi",
            name = "Double block",
            items = listOf(
                WorkoutItem.Repeat(
                    count = 4,
                    segments = listOf(
                        Segment("a1", SegmentType.RUN, SegmentLength.Time(240)),
                        Segment("a2", SegmentType.REST, SegmentLength.Time(120))
                    )
                ),
                WorkoutItem.Repeat(
                    count = 3,
                    segments = listOf(
                        Segment("b1", SegmentType.RUN, SegmentLength.Time(120))
                    )
                )
            )
        )

        assertThat(WorkoutSummary.intervalSummary(workout)).isEqualTo("4 × 4 min + 3 × 2 min")
    }

    @Test
    fun testDurationLabelRoundsToMinutesAndHandlesHours() {
        assertThat(WorkoutSummary.durationLabel(0)).isEqualTo("~0 s")
        assertThat(WorkoutSummary.durationLabel(59)).isEqualTo("~59 s")
        assertThat(WorkoutSummary.durationLabel(60)).isEqualTo("~1 min")
        assertThat(WorkoutSummary.durationLabel(3599)).isEqualTo("~1 h 00 min")
        assertThat(WorkoutSummary.durationLabel(3600)).isEqualTo("~1 h 00 min")
        assertThat(WorkoutSummary.durationLabel(3900)).isEqualTo("~1 h 05 min")
    }

    @Test
    fun testMetaLineCombinesSegmentsStructureAndDuration() {
        assertThat(WorkoutSummary.metaLine(WorkoutTemplates.repeats400m))
            .isEqualTo("18 segments · 8 × 400 m · ~41 min")

        // No repeats: segments and duration only (10 + 20 + 5 minutes).
        assertThat(WorkoutSummary.metaLine(WorkoutTemplates.tempoRun))
            .isEqualTo("3 segments · ~35 min")
    }
}
