package com.runningcompanion.app.domain.engine

import com.runningcompanion.app.domain.math.GeoUtils
import com.runningcompanion.app.domain.math.PaceCalculator
import com.runningcompanion.app.domain.model.GpsPoint
import com.runningcompanion.app.domain.model.RuntimeSegment
import com.runningcompanion.app.domain.model.SegmentLength
import com.runningcompanion.app.domain.model.SegmentResult
import com.runningcompanion.app.domain.model.SegmentType
import com.runningcompanion.app.domain.model.Session
import com.runningcompanion.app.domain.model.Workout

enum class RunnerState {
    IDLE,
    RUNNING,
    PAUSED,
    FINISHED
}

data class RunnerSnapshot(
    val state: RunnerState,
    val currentSegmentIndex: Int,
    val currentSegment: RuntimeSegment?,
    val totalSegments: Int,
    val segmentElapsedSec: Long,
    val segmentRemainingSec: Long?,
    val segmentDistanceM: Double,
    val segmentRemainingM: Double?,
    val totalDistanceM: Double,
    val totalDurationSec: Long,
    val currentPaceSecPerKm: Double?,
    val lastCue: CueEvent? = null
)

class WorkoutRunner(
    private val workout: Workout,
    private val paceCalculator: PaceCalculator = PaceCalculator(),
    private val cueScheduler: CueScheduler = CueScheduler()
) {
    private val runtimeSegments: List<RuntimeSegment> = workout.flatten()
    private var currentIndex = 0
    private var state: RunnerState = RunnerState.IDLE

    private var workoutStartTimestamp: Long = 0L
    private var segmentStartTimestamp: Long = 0L
    private var pauseStartTimestamp: Long = 0L
    private var segmentPausedMs: Long = 0L
    private var totalPausedMs: Long = 0L

    private var currentSegmentDistanceM: Double = 0.0
    private var totalDistanceM: Double = 0.0
    private var lastAcceptedPoint: GpsPoint? = null

    // Km split announcement state
    private var lastAnnouncedKm: Int = 0
    private var lastKmSplitTimeMs: Long = 0L
    private var lastKmSplitPausedMs: Long = 0L

    private val currentSegmentPoints = mutableListOf<GpsPoint>()
    private val allTrackPoints = mutableListOf<GpsPoint>()
    private val completedSegmentResults = mutableListOf<SegmentResult>()

    val activeState: RunnerState get() = state

    fun start(nowMs: Long): List<CueEvent> {
        check(state == RunnerState.IDLE) { "Cannot start runner from state $state" }
        state = RunnerState.RUNNING
        currentIndex = 0
        workoutStartTimestamp = nowMs
        segmentStartTimestamp = nowMs
        segmentPausedMs = 0L
        totalPausedMs = 0L
        currentSegmentDistanceM = 0.0
        totalDistanceM = 0.0
        lastAcceptedPoint = null
        lastAnnouncedKm = 0
        lastKmSplitTimeMs = nowMs
        lastKmSplitPausedMs = 0L
        currentSegmentPoints.clear()
        allTrackPoints.clear()
        completedSegmentResults.clear()
        cueScheduler.resetForNewSegment()

        return getCues(nowMs)
    }

    fun pause(nowMs: Long) {
        if (state == RunnerState.RUNNING) {
            state = RunnerState.PAUSED
            pauseStartTimestamp = nowMs
        }
    }

    fun resume(nowMs: Long) {
        if (state == RunnerState.PAUSED) {
            val pauseDuration = (nowMs - pauseStartTimestamp).coerceAtLeast(0L)
            segmentPausedMs += pauseDuration
            totalPausedMs += pauseDuration
            state = RunnerState.RUNNING
            // Reset previous point to prevent jump accumulation over paused gap
            lastAcceptedPoint = null
        }
    }

    fun skip(nowMs: Long): List<CueEvent> {
        if (state != RunnerState.RUNNING && state != RunnerState.PAUSED) return emptyList()
        return advanceSegment(nowMs)
    }

    fun stop(nowMs: Long): Session {
        if (state != RunnerState.FINISHED) {
            if (currentIndex < runtimeSegments.size && state != RunnerState.IDLE) {
                recordCurrentSegment(nowMs)
            }
            state = RunnerState.FINISHED
        }
        return buildSessionResult(nowMs)
    }

    fun onLocation(point: GpsPoint, nowMs: Long): List<CueEvent> {
        if (state != RunnerState.RUNNING) return emptyList()

        if (!GeoUtils.isPointAcceptable(point, lastAcceptedPoint)) {
            return emptyList()
        }

        val stepDist = GeoUtils.calculateAccumulatedDistance(point, lastAcceptedPoint)
        currentSegmentDistanceM += stepDist
        totalDistanceM += stepDist
        lastAcceptedPoint = point

        currentSegmentPoints.add(point)
        allTrackPoints.add(point)
        paceCalculator.addPoint(point)

        // Check if distance-based segment is complete
        val currentSeg = currentSegment ?: return emptyList()
        val len = currentSeg.segment.length

        // Check if a 1-km milestone was reached
        val kmCues = checkKmSplit(nowMs)

        if (len is SegmentLength.Distance && currentSegmentDistanceM >= len.meters) {
            return kmCues + advanceSegment(nowMs)
        }

        return kmCues + getCues(nowMs)
    }

    private fun checkKmSplit(nowMs: Long): List<CueEvent> {
        val currentKm = (totalDistanceM / 1000.0).toInt()
        if (currentKm > lastAnnouncedKm && currentKm >= 1) {
            val completedKm = currentKm
            lastAnnouncedKm = currentKm

            // Active elapsed time for this completed kilometer
            val pausedDuringKm = (totalPausedMs - lastKmSplitPausedMs).coerceAtLeast(0L)
            val timeForKmMs = (nowMs - lastKmSplitTimeMs - pausedDuringKm).coerceAtLeast(1000L)
            val splitPaceSec = timeForKmMs / 1000.0 // 1 km pace is precisely time for 1 km

            lastKmSplitTimeMs = nowMs
            lastKmSplitPausedMs = totalPausedMs

            val ongoingPause = if (state == RunnerState.PAUSED) (nowMs - pauseStartTimestamp).coerceAtLeast(0L) else 0L
            val totalActiveDurationSec = ((nowMs - workoutStartTimestamp - totalPausedMs - ongoingPause) / 1000L).coerceAtLeast(0L)

            return listOf(
                CueEvent.KmSplitAlert(
                    kilometer = completedKm,
                    splitPaceSecPerKm = splitPaceSec,
                    totalDurationSec = totalActiveDurationSec
                )
            )
        }
        return emptyList()
    }

    fun tick(nowMs: Long): List<CueEvent> {
        if (state != RunnerState.RUNNING) return emptyList()

        val currentSeg = currentSegment ?: return emptyList()
        val len = currentSeg.segment.length

        // Check if time-based segment is complete
        if (len is SegmentLength.Time) {
            val elapsedSec = computeSegmentElapsedSec(nowMs)
            if (elapsedSec >= len.seconds) {
                return advanceSegment(nowMs)
            }
        }

        return getCues(nowMs)
    }

    private fun advanceSegment(nowMs: Long): List<CueEvent> {
        val cues = mutableListOf<CueEvent>()
        val finishedSeg = currentSegment

        recordCurrentSegment(nowMs)

        // Check if rest segment finished inside a repeat group
        if (finishedSeg != null && finishedSeg.segment.type == SegmentType.REST) {
            val setNumber = finishedSeg.setNumber
            val setTotal = finishedSeg.setTotal
            if (setNumber != null && setTotal != null) {
                cueScheduler.onRestSegmentEnd(setNumber, setTotal)?.let { cues.add(it) }
            }
        }

        currentIndex++
        if (currentIndex >= runtimeSegments.size) {
            state = RunnerState.FINISHED
            cues.add(CueEvent.WorkoutComplete)
        } else {
            // Setup next segment
            segmentStartTimestamp = nowMs
            segmentPausedMs = 0L
            currentSegmentDistanceM = 0.0
            lastAcceptedPoint = null
            currentSegmentPoints.clear()
            cueScheduler.resetForNewSegment()
            cues.addAll(getCues(nowMs))
        }

        return cues
    }

    private fun recordCurrentSegment(nowMs: Long) {
        val seg = currentSegment ?: return
        val durationSec = computeSegmentElapsedSec(nowMs)
        val avgPace = if (currentSegmentDistanceM >= 5.0) {
            durationSec / (currentSegmentDistanceM / 1000.0)
        } else {
            null
        }

        val splits = if (seg.segment.type == SegmentType.WARMUP || seg.segment.type == SegmentType.COOLDOWN) {
            ResultsCalculator.calculateKmSplits(currentSegmentPoints)
        } else {
            emptyList()
        }

        completedSegmentResults.add(
            SegmentResult(
                index = seg.index,
                type = seg.segment.type,
                setNumber = seg.setNumber,
                startedAt = segmentStartTimestamp,
                endedAt = nowMs,
                durationSec = durationSec,
                distanceM = currentSegmentDistanceM,
                avgPaceSecPerKm = avgPace,
                kmSplits = splits,
                elevationGainM = ResultsCalculator.calculateElevationGain(currentSegmentPoints)
            )
        )
    }

    private fun getCues(nowMs: Long): List<CueEvent> {
        val seg = currentSegment ?: return emptyList()
        val elapsedSec = computeSegmentElapsedSec(nowMs)
        val pace = paceCalculator.calculateLivePaceSecPerKm(nowMs)
        return cueScheduler.checkCues(
            segment = seg,
            segmentElapsedSec = elapsedSec,
            segmentDistanceM = currentSegmentDistanceM,
            currentPaceSecPerKm = pace,
            nowMs = nowMs
        )
    }

    private fun computeSegmentElapsedSec(nowMs: Long): Long {
        if (state == RunnerState.IDLE) return 0L
        val ongoingPause = if (state == RunnerState.PAUSED) (nowMs - pauseStartTimestamp).coerceAtLeast(0L) else 0L
        val activeMs = (nowMs - segmentStartTimestamp - segmentPausedMs - ongoingPause).coerceAtLeast(0L)
        return activeMs / 1000L
    }

    fun getSnapshot(nowMs: Long): RunnerSnapshot {
        val seg = currentSegment
        val segElapsedSec = computeSegmentElapsedSec(nowMs)
        val livePace = paceCalculator.calculateLivePaceSecPerKm(nowMs)

        var remSec: Long? = null
        var remMeters: Double? = null

        if (seg != null) {
            when (val len = seg.segment.length) {
                is SegmentLength.Time -> {
                    remSec = (len.seconds - segElapsedSec).coerceAtLeast(0L)
                }
                is SegmentLength.Distance -> {
                    remMeters = (len.meters - currentSegmentDistanceM).coerceAtLeast(0.0)
                }
            }
        }

        val ongoingPause = if (state == RunnerState.PAUSED) (nowMs - pauseStartTimestamp).coerceAtLeast(0L) else 0L
        val totalDuration = if (workoutStartTimestamp > 0L) {
            ((nowMs - workoutStartTimestamp - totalPausedMs - ongoingPause) / 1000L).coerceAtLeast(0L)
        } else 0L

        return RunnerSnapshot(
            state = state,
            currentSegmentIndex = currentIndex,
            currentSegment = seg,
            totalSegments = runtimeSegments.size,
            segmentElapsedSec = segElapsedSec,
            segmentRemainingSec = remSec,
            segmentDistanceM = currentSegmentDistanceM,
            segmentRemainingM = remMeters,
            totalDistanceM = totalDistanceM,
            totalDurationSec = totalDuration,
            currentPaceSecPerKm = livePace
        )
    }

    fun buildSessionResult(nowMs: Long): Session {
        return ResultsCalculator.buildSession(
            workoutSnapshot = workout,
            startedAt = workoutStartTimestamp,
            endedAt = nowMs,
            segmentResults = completedSegmentResults.toList(),
            allTrackPoints = allTrackPoints.toList()
        )
    }

    val currentSegment: RuntimeSegment?
        get() = if (currentIndex in runtimeSegments.indices) runtimeSegments[currentIndex] else null
}
