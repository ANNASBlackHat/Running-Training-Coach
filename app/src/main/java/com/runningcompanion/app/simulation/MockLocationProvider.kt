package com.runningcompanion.app.simulation

import com.runningcompanion.app.domain.model.GpsPoint

class MockLocationProvider(
    private val startLat: Double = -6.200000,
    private val startLon: Double = 106.816666,
    private val basePaceSecPerKm: Double = 300.0 // 5:00 min/km = 3.33 m/s
) {
    private var currentLat = startLat
    private var currentLon = startLon
    private var currentTimeMs = 1_000_000L

    // In 1 second at paceSecPerKm, distance in meters is 1000.0 / paceSecPerKm
    fun nextPoint(
        intervalMs: Long = 1000L,
        customPaceSecPerKm: Double? = null,
        accuracy: Float = 5.0f
    ): GpsPoint {
        currentTimeMs += intervalMs
        val pace = customPaceSecPerKm ?: basePaceSecPerKm
        val speedMps = 1000.0 / pace
        val distanceTraveledMeters = speedMps * (intervalMs / 1000.0)

        // Move northward: 1 degree latitude is approx 111,320 meters
        val latDelta = distanceTraveledMeters / 111320.0
        currentLat += latDelta

        return GpsPoint(
            timestamp = currentTimeMs,
            latitude = currentLat,
            longitude = currentLon,
            accuracy = accuracy,
            speed = speedMps.toFloat(),
            altitude = 15.0
        )
    }

    val currentTimestamp: Long get() = currentTimeMs
}
