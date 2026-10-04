package com.runningcompanion.app.domain.engine

import com.runningcompanion.app.domain.math.GeoUtils
import com.runningcompanion.app.domain.model.GpsPoint
import com.runningcompanion.app.domain.model.KmSplit
import com.runningcompanion.app.domain.model.SegmentResult
import com.runningcompanion.app.domain.model.SegmentType
import com.runningcompanion.app.domain.model.Session
import com.runningcompanion.app.domain.model.SessionTotals
import com.runningcompanion.app.domain.model.Workout
import java.util.UUID

object ResultsCalculator {

    fun calculateKmSplits(points: List<GpsPoint>): List<KmSplit> {
        if (points.size < 2) return emptyList()

        val splits = mutableListOf<KmSplit>()
        var accumulatedDistMeters = 0.0
        var currentKm = 1
        var kmStartTimestamp = points.first().timestamp
        var prevPoint = points.first()

        for (i in 1 until points.size) {
            val point = points[i]
            val stepDist = GeoUtils.haversineDistanceMeters(
                prevPoint.latitude,
                prevPoint.longitude,
                point.latitude,
                point.longitude
            )
            accumulatedDistMeters += stepDist

            val targetDist = currentKm * 1000.0
            if (accumulatedDistMeters >= targetDist) {
                // Interpolate split timestamp
                val overshoot = accumulatedDistMeters - targetDist
                val ratio = if (stepDist > 0) (stepDist - overshoot) / stepDist else 1.0
                val splitTimestamp = prevPoint.timestamp + ((point.timestamp - prevPoint.timestamp) * ratio).toLong()

                val durationSec = (splitTimestamp - kmStartTimestamp) / 1000.0
                val pace = durationSec // 1 km duration in seconds is pace in sec/km
                splits.add(KmSplit(km = currentKm, paceSecPerKm = pace))

                kmStartTimestamp = splitTimestamp
                currentKm++
            }
            prevPoint = point
        }

        return splits
    }

    fun calculateElevationGain(points: List<GpsPoint>): Double {
        var gain = 0.0
        var prevAltitude: Double? = null
        for (p in points) {
            val alt = p.altitude
            if (alt != null) {
                if (prevAltitude != null && alt > prevAltitude) {
                    val diff = alt - prevAltitude
                    if (diff < 50.0) { // filter out massive GPS altitude glitches
                        gain += diff
                    }
                }
                prevAltitude = alt
            }
        }
        return gain
    }

    fun buildSession(
        workoutSnapshot: Workout,
        startedAt: Long,
        endedAt: Long,
        segmentResults: List<SegmentResult>,
        allTrackPoints: List<GpsPoint>
    ): Session {
        var totalDurationSec = 0L
        var totalDistanceM = 0.0

        for (res in segmentResults) {
            totalDurationSec += res.durationSec
            totalDistanceM += res.distanceM
        }

        val overallAvgPace = if (totalDistanceM >= 5.0) {
            totalDurationSec / (totalDistanceM / 1000.0)
        } else {
            null
        }

        return Session(
            id = UUID.randomUUID().toString(),
            workoutId = workoutSnapshot.id,
            workoutSnapshot = workoutSnapshot,
            startedAt = startedAt,
            endedAt = endedAt,
            results = segmentResults,
            totals = SessionTotals(
                durationSec = totalDurationSec,
                distanceM = totalDistanceM,
                avgPaceSecPerKm = overallAvgPace
            ),
            track = allTrackPoints
        )
    }
}
