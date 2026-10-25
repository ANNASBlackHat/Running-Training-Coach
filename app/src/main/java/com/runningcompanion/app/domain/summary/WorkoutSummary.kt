package com.runningcompanion.app.domain.summary

import com.runningcompanion.app.domain.model.RuntimeSegment
import com.runningcompanion.app.domain.model.SegmentLength
import com.runningcompanion.app.domain.model.SegmentType
import com.runningcompanion.app.domain.model.Workout
import com.runningcompanion.app.domain.model.WorkoutItem

/**
 * Read-only descriptions of a workout used by list surfaces (home, history).
 * Duration estimates for distance segments use the segment's target pace range
 * when one exists, falling back to [DEFAULT_PACE_SEC_PER_KM] otherwise.
 */
object WorkoutSummary {

    /**
     * Baseline pace used to estimate the cost of distance-based segments that
     * carry no explicit target (roughly 5:30 min/km).
     */
    const val DEFAULT_PACE_SEC_PER_KM = 330.0

    fun estimatedSeconds(segment: RuntimeSegment): Double {
        return when (val length = segment.segment.length) {
            is SegmentLength.Time -> length.seconds.toDouble()
            is SegmentLength.Distance -> {
                val pace = segment.segment.paceRange
                    ?.let { (it.fastSecPerKm + it.slowSecPerKm) / 2.0 }
                    ?: DEFAULT_PACE_SEC_PER_KM
                (length.meters / 1000.0) * pace
            }
        }
    }

    fun estimatedSeconds(segments: List<RuntimeSegment>): Long {
        return segments.sumOf { estimatedSeconds(it) }.toLong()
    }

    fun estimatedSeconds(workout: Workout): Long = estimatedSeconds(workout.flatten())

    /**
     * Describes the repeated work intervals, e.g. "8 × 400 m" or "4 × 4 min".
     * Returns null when the workout has no repeat blocks.
     */
    fun intervalSummary(workout: Workout): String? {
        val repeats = workout.items.filterIsInstance<WorkoutItem.Repeat>()
        if (repeats.isEmpty()) return null

        return repeats
            .mapNotNull { repeat ->
                val work = repeat.segments.firstOrNull { it.type == SegmentType.RUN }
                    ?: repeat.segments.firstOrNull()
                    ?: return@mapNotNull null
                "${repeat.count} × ${formatLength(work.length)}"
            }
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" + ")
    }

    /** Round duration to a human label, e.g. "~34 min" or "~1 h 05 min". */
    fun durationLabel(totalSeconds: Long): String {
        val safeSeconds = totalSeconds.coerceAtLeast(0L)
        if (safeSeconds < 60L) return "~${safeSeconds} s"

        val totalMinutes = (safeSeconds + 30L) / 60L
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return if (hours > 0L) {
            "~$hours h %02d min".format(minutes)
        } else {
            "~$minutes min"
        }
    }

    /**
     * The line shown under a workout name, e.g.
     * "18 segments · 8 × 400 m · ~42 min".
     */
    fun metaLine(workout: Workout): String {
        val segments = workout.flatten()
        val parts = mutableListOf("${segments.size} segments")
        intervalSummary(workout)?.let { parts.add(it) }
        parts.add(durationLabel(estimatedSeconds(segments)))
        return parts.joinToString(" · ")
    }

    /** "4 min" for whole minutes, "90 s" otherwise, "400 m" for distance. */
    private fun formatLength(length: SegmentLength): String {
        return when (length) {
            is SegmentLength.Distance -> "${length.meters} m"
            is SegmentLength.Time -> {
                if (length.seconds % 60 == 0) {
                    "${length.seconds / 60} min"
                } else {
                    "${length.seconds} s"
                }
            }
        }
    }
}
