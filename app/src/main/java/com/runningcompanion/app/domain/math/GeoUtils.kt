package com.runningcompanion.app.domain.math

import com.runningcompanion.app.domain.model.GpsPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeoUtils {
    private const val EARTH_RADIUS_METERS = 6371000.0
    const val MAX_ACCURACY_THRESHOLD_METERS = 25.0f
    const val MAX_RUNNING_SPEED_MPS = 10.0 // ~36 km/h
    const val MIN_DISPLACEMENT_METERS = 1.0

    fun haversineDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val latDistance = Math.toRadians(lat2 - lat1)
        val lonDistance = Math.toRadians(lon2 - lon1)
        val a = sin(latDistance / 2) * sin(latDistance / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(lonDistance / 2) * sin(lonDistance / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    fun isPointAcceptable(
        point: GpsPoint,
        previousPoint: GpsPoint?
    ): Boolean {
        if (point.accuracy > MAX_ACCURACY_THRESHOLD_METERS) {
            return false
        }
        if (previousPoint != null) {
            val timeDeltaSec = (point.timestamp - previousPoint.timestamp) / 1000.0
            if (timeDeltaSec > 0) {
                val distance = haversineDistanceMeters(
                    previousPoint.latitude,
                    previousPoint.longitude,
                    point.latitude,
                    point.longitude
                )
                val impliedSpeed = distance / timeDeltaSec
                if (impliedSpeed > MAX_RUNNING_SPEED_MPS) {
                    return false
                }
            }
        }
        return true
    }

    fun calculateAccumulatedDistance(
        currentPoint: GpsPoint,
        previousPoint: GpsPoint?
    ): Double {
        if (previousPoint == null) return 0.0
        val dist = haversineDistanceMeters(
            previousPoint.latitude,
            previousPoint.longitude,
            currentPoint.latitude,
            currentPoint.longitude
        )
        // If moving less than 1m and reported speed is very low, suppress standing-still drift
        if (dist < MIN_DISPLACEMENT_METERS && (currentPoint.speed ?: 0f) < 0.5f) {
            return 0.0
        }
        return dist
    }
}
