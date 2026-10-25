package com.runningcompanion.app.domain.share

import com.runningcompanion.app.domain.model.GpsPoint
import com.runningcompanion.app.domain.model.SegmentType
import com.runningcompanion.app.domain.model.Session
import com.runningcompanion.app.domain.summary.WorkoutSummary

/**
 * One bar in the interval chart of a share card: a RUN segment with its
 * achieved pace and the target pace band defined on the workout.
 */
data class IntervalBar(
    val label: String,
    val paceSecPerKm: Double,
    val targetFastSecPerKm: Double?,
    val targetSlowSecPerKm: Double?
) {
    val isOutOfRange: Boolean
        get() = targetFastSecPerKm != null &&
            targetSlowSecPerKm != null &&
            (paceSecPerKm < targetFastSecPerKm || paceSecPerKm > targetSlowSecPerKm)
}

/** Pacing stats across the work intervals of a session. */
data class Consistency(
    val fastestSecPerKm: Double,
    val slowestSecPerKm: Double,
    val cvPercent: Double
)

/** One row of the lap table. */
data class LapRow(
    val type: SegmentType,
    val setLabel: String?,
    val durationSec: Long,
    val distanceM: Double,
    val paceSecPerKm: Double?
) {
    val typeLabel: String
        get() = when (type) {
            SegmentType.RUN -> "Run"
            SegmentType.REST -> "Rest"
            SegmentType.WARMUP -> "Warm-up"
            SegmentType.COOLDOWN -> "Cool-down"
        }
}

/**
 * Pure, pre-formatted-ready view model for a share card. Built from a
 * [Session]; contains everything the renderer needs but no Android types.
 */
data class ShareCardModel(
    val workoutName: String,
    val startedAt: Long,
    val intervalTag: String?,
    val distanceM: Double,
    val durationSec: Long,
    val avgPaceSecPerKm: Double?,
    val elevationGainM: Double?,
    val fastestSetPaceSecPerKm: Double?,
    val hasTrack: Boolean,
    val track: List<GpsPoint>,
    val bars: List<IntervalBar>,
    val consistency: Consistency?,
    val laps: List<LapRow>,
    val lapTruncationCount: Int
) {
    companion object {

        const val MAX_VISIBLE_LAPS = 14

        fun fromSession(session: Session): ShareCardModel {
            val flattened = session.workoutSnapshot.flatten()

            val bars = session.results
                .filter { it.type == SegmentType.RUN }
                .mapNotNull { res ->
                    val pace = res.avgPaceSecPerKm ?: return@mapNotNull null
                    val target = if (res.index in flattened.indices) {
                        flattened[res.index].segment.paceRange
                    } else {
                        null
                    }
                    IntervalBar(
                        label = res.setNumber?.let { "S$it" } ?: "Run",
                        paceSecPerKm = pace,
                        targetFastSecPerKm = target?.fastSecPerKm?.toDouble(),
                        targetSlowSecPerKm = target?.slowSecPerKm?.toDouble()
                    )
                }

            val consistency = if (bars.size >= 2) {
                val paces = bars.map { it.paceSecPerKm }
                val fastest = paces.min()
                val slowest = paces.max()
                val mean = paces.average()
                val stdev = kotlin.math.sqrt(paces.map { (it - mean) * (it - mean) }.average())
                Consistency(
                    fastestSecPerKm = fastest,
                    slowestSecPerKm = slowest,
                    cvPercent = if (mean > 0) stdev / mean * 100.0 else 0.0
                )
            } else {
                null
            }

            val allLaps = session.results.map { res ->
                val flat = flattened.getOrNull(res.index)
                val setLabel = res.setNumber?.let { n ->
                    flat?.setTotal?.let { "$n/$it" } ?: n.toString()
                }
                LapRow(
                    type = res.type,
                    setLabel = setLabel,
                    durationSec = res.durationSec,
                    distanceM = res.distanceM,
                    paceSecPerKm = res.avgPaceSecPerKm
                )
            }

            val elevationSums = session.results.mapNotNull { it.elevationGainM }
            val elevationGainM = if (elevationSums.isNotEmpty() && elevationSums.sum() > 0.0) {
                elevationSums.sum()
            } else {
                null
            }

            return ShareCardModel(
                workoutName = session.workoutSnapshot.name,
                startedAt = session.startedAt,
                intervalTag = buildIntervalTag(session),
                distanceM = session.totals.distanceM,
                durationSec = session.totals.durationSec,
                avgPaceSecPerKm = session.totals.avgPaceSecPerKm,
                elevationGainM = elevationGainM,
                fastestSetPaceSecPerKm = consistency?.fastestSecPerKm,
                hasTrack = session.track.isNotEmpty(),
                track = session.track,
                bars = bars,
                consistency = consistency,
                laps = allLaps.take(MAX_VISIBLE_LAPS),
                lapTruncationCount = (allLaps.size - MAX_VISIBLE_LAPS).coerceAtLeast(0)
            )
        }

        private fun buildIntervalTag(session: Session): String? {
            val intervals = WorkoutSummary.intervalSummary(session.workoutSnapshot) ?: return null
            val estimatedSec = WorkoutSummary.estimatedSeconds(session.workoutSnapshot.flatten())
            return "$intervals · ${WorkoutSummary.durationLabel(estimatedSec)}"
        }
    }
}
