package com.runningcompanion.app.domain.engine

import com.runningcompanion.app.domain.model.PaceRange
import com.runningcompanion.app.domain.model.RuntimeSegment
import com.runningcompanion.app.domain.model.SegmentLength
import com.runningcompanion.app.domain.model.SegmentType

sealed interface CueEvent {
    val isHighPriority: Boolean

    data class SegmentStart(
        val type: SegmentType,
        val length: SegmentLength,
        val setNumber: Int?,
        val setTotal: Int?
    ) : CueEvent {
        override val isHighPriority: Boolean = true
    }

    data class Countdown(val secondsRemaining: Int) : CueEvent {
        override val isHighPriority: Boolean = true
    }

    object Halfway : CueEvent {
        override val isHighPriority: Boolean = false
    }

    data class TimeRemaining(val secondsLeft: Int) : CueEvent {
        override val isHighPriority: Boolean = false
    }

    data class DistanceRemaining(val metersLeft: Int) : CueEvent {
        override val isHighPriority: Boolean = false
    }

    enum class PaceAlertDirection {
        TOO_FAST,
        TOO_SLOW
    }

    data class PaceAlert(
        val direction: PaceAlertDirection,
        val currentPaceSecPerKm: Double,
        val paceRange: PaceRange
    ) : CueEvent {
        override val isHighPriority: Boolean = false
    }

    data class BackOnPace(
        val currentPaceSecPerKm: Double,
        val paceRange: PaceRange
    ) : CueEvent {
        override val isHighPriority: Boolean = false
    }

    data class KmSplitAlert(
        val kilometer: Int,
        val splitPaceSecPerKm: Double,
        val totalDurationSec: Long
    ) : CueEvent {
        override val isHighPriority: Boolean = false
    }

    data class SetsLeft(val setsRemaining: Int) : CueEvent {
        override val isHighPriority: Boolean = false
    }

    object WorkoutComplete : CueEvent {
        override val isHighPriority: Boolean = true
    }
}

class CueScheduler {
    private var firedStartCue = false
    private var firedHalfwayCue = false
    private var firedRemainingCue = false
    private val firedCountdownSeconds = mutableSetOf<Int>()

    // Pace alert state
    private var outOfRangeStartTimestamp: Long? = null
    private var lastPaceAlertTimestamp: Long = 0L
    private var lastPaceAlertDirection: CueEvent.PaceAlertDirection? = null
    private var isCurrentlyOutOfRange: Boolean = false
    private var inRangeStartTimestamp: Long? = null

    fun resetForNewSegment() {
        firedStartCue = false
        firedHalfwayCue = false
        firedRemainingCue = false
        firedCountdownSeconds.clear()
        outOfRangeStartTimestamp = null
        isCurrentlyOutOfRange = false
        inRangeStartTimestamp = null
    }

    fun checkCues(
        segment: RuntimeSegment,
        segmentElapsedSec: Long,
        segmentDistanceM: Double,
        currentPaceSecPerKm: Double?,
        nowMs: Long,
        totalSets: Int? = null
    ): List<CueEvent> {
        val events = mutableListOf<CueEvent>()

        // 1. Segment start cue
        if (!firedStartCue) {
            firedStartCue = true
            events.add(
                CueEvent.SegmentStart(
                    type = segment.segment.type,
                    length = segment.segment.length,
                    setNumber = segment.setNumber,
                    setTotal = segment.setTotal
                )
            )
        }

        when (val len = segment.segment.length) {
            is SegmentLength.Time -> {
                val totalSec = len.seconds
                val remainingSec = (totalSec - segmentElapsedSec).coerceAtLeast(0)

                // Halfway cue (if >= 120s)
                if (!firedHalfwayCue && totalSec >= 120 && segmentElapsedSec >= totalSec / 2) {
                    firedHalfwayCue = true
                    events.add(CueEvent.Halfway)
                }

                // Remaining cue (60s left if total > 180s)
                if (!firedRemainingCue && totalSec > 180 && remainingSec in 58..60) {
                    firedRemainingCue = true
                    events.add(CueEvent.TimeRemaining(60))
                }

                // Countdown beeps (3, 2, 1)
                if (remainingSec in 1..3 && !firedCountdownSeconds.contains(remainingSec.toInt())) {
                    firedCountdownSeconds.add(remainingSec.toInt())
                    events.add(CueEvent.Countdown(remainingSec.toInt()))
                }
            }
            is SegmentLength.Distance -> {
                val totalMeters = len.meters
                val remainingMeters = (totalMeters - segmentDistanceM).coerceAtLeast(0.0)

                // Halfway cue (if >= 400m)
                if (!firedHalfwayCue && totalMeters >= 400 && segmentDistanceM >= totalMeters / 2.0) {
                    firedHalfwayCue = true
                    events.add(CueEvent.Halfway)
                }

                // Remaining cue (100m left if total > 400m)
                if (!firedRemainingCue && totalMeters > 400 && remainingMeters in 80.0..100.0) {
                    firedRemainingCue = true
                    events.add(CueEvent.DistanceRemaining(100))
                }
            }
        }

        // Pace alert check
        val paceAlert = checkPaceAlert(segment.segment.paceRange, currentPaceSecPerKm, segmentElapsedSec, nowMs)
        if (paceAlert != null) {
            events.add(paceAlert)
        }

        return events
    }

    private fun checkPaceAlert(
        paceRange: PaceRange?,
        currentPaceSecPerKm: Double?,
        segmentElapsedSec: Long,
        nowMs: Long
    ): CueEvent? {
        if (paceRange == null || currentPaceSecPerKm == null) {
            outOfRangeStartTimestamp = null
            inRangeStartTimestamp = null
            return null
        }

        // Extended grace period: 25s post-transition before any pace alerts fire
        if (segmentElapsedSec < 25) {
            outOfRangeStartTimestamp = null
            inRangeStartTimestamp = null
            return null
        }

        val direction = when {
            paceRange.isTooFast(currentPaceSecPerKm) -> CueEvent.PaceAlertDirection.TOO_FAST
            paceRange.isTooSlow(currentPaceSecPerKm) -> CueEvent.PaceAlertDirection.TOO_SLOW
            else -> null
        }

        if (direction != null) {
            // Out of range
            inRangeStartTimestamp = null
            val startTime = outOfRangeStartTimestamp
            if (startTime == null) {
                outOfRangeStartTimestamp = nowMs
                return null
            } else if (nowMs - startTime < 7_800L) {
                return null
            }

            // Mark that runner is confirmed out of range
            isCurrentlyOutOfRange = true

            // Cooldown: at most one alert every 25 seconds
            if (nowMs - lastPaceAlertTimestamp < 25_000L) {
                return null
            }

            lastPaceAlertTimestamp = nowMs
            lastPaceAlertDirection = direction
            outOfRangeStartTimestamp = null
            return CueEvent.PaceAlert(
                direction = direction,
                currentPaceSecPerKm = currentPaceSecPerKm,
                paceRange = paceRange
            )
        } else {
            // In range!
            outOfRangeStartTimestamp = null

            if (isCurrentlyOutOfRange) {
                val inStartTime = inRangeStartTimestamp
                if (inStartTime == null) {
                    inRangeStartTimestamp = nowMs
                    return null
                } else if (nowMs - inStartTime >= 3_000L) {
                    // Runner has maintained target pace for 3+ seconds!
                    isCurrentlyOutOfRange = false
                    inRangeStartTimestamp = null
                    lastPaceAlertTimestamp = nowMs // prevent immediate alert fluctuation
                    return CueEvent.BackOnPace(
                        currentPaceSecPerKm = currentPaceSecPerKm,
                        paceRange = paceRange
                    )
                }
            }
            return null
        }
    }

    fun onRestSegmentEnd(currentSet: Int, totalSets: Int): CueEvent.SetsLeft? {
        val setsLeft = totalSets - currentSet
        return if (setsLeft > 0) CueEvent.SetsLeft(setsLeft) else null
    }
}
