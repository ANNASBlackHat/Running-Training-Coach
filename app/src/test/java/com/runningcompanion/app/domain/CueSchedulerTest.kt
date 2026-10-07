package com.runningcompanion.app.domain

import com.google.common.truth.Truth.assertThat
import com.runningcompanion.app.domain.engine.CueEvent
import com.runningcompanion.app.domain.engine.CueScheduler
import com.runningcompanion.app.domain.model.PaceRange
import com.runningcompanion.app.domain.model.RuntimeSegment
import com.runningcompanion.app.domain.model.Segment
import com.runningcompanion.app.domain.model.SegmentLength
import com.runningcompanion.app.domain.model.SegmentType
import org.junit.Before
import org.junit.Test

class CueSchedulerTest {

    private lateinit var scheduler: CueScheduler
    private val testSegment = RuntimeSegment(
        index = 0,
        segment = Segment(
            id = "seg1",
            type = SegmentType.RUN,
            length = SegmentLength.Time(240), // 4 min
            paceRange = PaceRange(fastSecPerKm = 270, slowSecPerKm = 300) // 4:30 - 5:00 min/km
        ),
        setNumber = 1,
        setTotal = 4
    )

    @Before
    fun setup() {
        scheduler = CueScheduler()
    }

    @Test
    fun testSegmentStartCue_firesOnceAtBeginning() {
        val cues = scheduler.checkCues(
            segment = testSegment,
            segmentElapsedSec = 0,
            segmentDistanceM = 0.0,
            currentPaceSecPerKm = null,
            nowMs = 1000L
        )

        assertThat(cues).hasSize(1)
        assertThat(cues.first()).isInstanceOf(CueEvent.SegmentStart::class.java)

        val nextCues = scheduler.checkCues(
            segment = testSegment,
            segmentElapsedSec = 1,
            segmentDistanceM = 3.0,
            currentPaceSecPerKm = null,
            nowMs = 2000L
        )
        // Should not repeat SegmentStart
        assertThat(nextCues.any { it is CueEvent.SegmentStart }).isFalse()
    }

    @Test
    fun testHalfwayAndRemainingCues() {
        // Run until halfway (120s of 240s)
        scheduler.checkCues(testSegment, 0, 0.0, null, 1000L)
        val cuesAtHalfway = scheduler.checkCues(testSegment, 120, 400.0, 300.0, 120_000L)
        assertThat(cuesAtHalfway.any { it is CueEvent.Halfway }).isTrue()

        // 60s remaining (elapsed = 180s of 240s)
        val cuesAt60s = scheduler.checkCues(testSegment, 180, 600.0, 300.0, 180_000L)
        assertThat(cuesAt60s.any { it is CueEvent.TimeRemaining }).isTrue()
    }

    @Test
    fun testCountdownBeeps_lastThreeSeconds() {
        scheduler.checkCues(testSegment, 0, 0.0, null, 1000L)

        val cue3 = scheduler.checkCues(testSegment, 237, 790.0, 300.0, 237_000L)
        val countdown3 = cue3.filterIsInstance<CueEvent.Countdown>()
        assertThat(countdown3).hasSize(1)
        assertThat(countdown3.first().secondsRemaining).isEqualTo(3)

        val cue2 = scheduler.checkCues(testSegment, 238, 793.0, 300.0, 238_000L)
        val countdown2 = cue2.filterIsInstance<CueEvent.Countdown>()
        assertThat(countdown2).hasSize(1)
        assertThat(countdown2.first().secondsRemaining).isEqualTo(2)

        val cue1 = scheduler.checkCues(testSegment, 239, 796.0, 300.0, 239_000L)
        val countdown1 = cue1.filterIsInstance<CueEvent.Countdown>()
        assertThat(countdown1).hasSize(1)
        assertThat(countdown1.first().secondsRemaining).isEqualTo(1)
    }

    @Test
    fun testPaceAlerts_respectsGracePeriodAndCooldown() {
        scheduler.checkCues(testSegment, 0, 0.0, null, 1000L)

        // During first 25s grace period: too fast (e.g. 240 sec/km < 270), but no alert allowed
        val graceCues = scheduler.checkCues(
            segment = testSegment,
            segmentElapsedSec = 20,
            segmentDistanceM = 70.0,
            currentPaceSecPerKm = 240.0,
            nowMs = 20_000L
        )
        assertThat(graceCues.any { it is CueEvent.PaceAlert }).isFalse()

        // At 26s (after grace): pace is too fast (240 s/km)
        scheduler.checkCues(testSegment, 26, 90.0, 240.0, 26_000L)
        // At 34s (8s continuous out of range): alert should fire!
        val alertCues = scheduler.checkCues(testSegment, 34, 120.0, 240.0, 34_000L)
        val alert = alertCues.filterIsInstance<CueEvent.PaceAlert>()
        assertThat(alert).hasSize(1)
        val paceAlert = alert.first()
        assertThat(paceAlert.direction).isEqualTo(CueEvent.PaceAlertDirection.TOO_FAST)
        assertThat(paceAlert.currentPaceSecPerKm).isEqualTo(240.0)
        assertThat(paceAlert.paceRange.fastSecPerKm).isEqualTo(270)

        // At 40s (only 6s after previous alert): cooldown must suppress it
        val cooldownCues = scheduler.checkCues(testSegment, 40, 150.0, 240.0, 40_000L)
        assertThat(cooldownCues.any { it is CueEvent.PaceAlert }).isFalse()
    }

    @Test
    fun testBackOnPaceCue_firesWhenRunnerReturnsToTargetRange() {
        scheduler.checkCues(testSegment, 0, 0.0, null, 1000L)

        // Trigger an out-of-range alert first
        scheduler.checkCues(testSegment, 26, 90.0, 240.0, 26_000L)
        val alertCues = scheduler.checkCues(testSegment, 34, 120.0, 240.0, 34_000L)
        assertThat(alertCues.any { it is CueEvent.PaceAlert }).isTrue()

        // Runner adjusts pace back to target (e.g. 285s / km, inside 270..300)
        val recoveryImmediate = scheduler.checkCues(testSegment, 35, 125.0, 285.0, 35_000L)
        assertThat(recoveryImmediate.any { it is CueEvent.BackOnPace }).isFalse()

        // After sustaining target pace for 3+ seconds (35s -> 38s)
        val recoverySustained = scheduler.checkCues(testSegment, 38, 140.0, 285.0, 38_000L)
        val backOnPace = recoverySustained.filterIsInstance<CueEvent.BackOnPace>()
        assertThat(backOnPace).hasSize(1)
        assertThat(backOnPace.first().currentPaceSecPerKm).isEqualTo(285.0)

        // Subsequent ticks in range do NOT repeat BackOnPace
        val subsequent = scheduler.checkCues(testSegment, 39, 145.0, 285.0, 39_000L)
        assertThat(subsequent.any { it is CueEvent.BackOnPace }).isFalse()
    }
}
