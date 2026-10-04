package com.runningcompanion.app.domain.math

import com.runningcompanion.app.domain.model.GpsPoint

class PaceCalculator(
    private val windowDurationMs: Long = 15_000L,
    private val minDistanceThresholdMeters: Double = 5.0
) {
    private val windowPoints = mutableListOf<GpsPoint>()

    fun addPoint(point: GpsPoint) {
        windowPoints.add(point)
        prune(point.timestamp)
    }

    fun clear() {
        windowPoints.clear()
    }

    private fun prune(currentTimestamp: Long) {
        val cutoff = currentTimestamp - windowDurationMs
        while (windowPoints.isNotEmpty() && windowPoints.first().timestamp < cutoff) {
            windowPoints.removeAt(0)
        }
    }

    fun calculateLivePaceSecPerKm(currentTimestamp: Long): Double? {
        prune(currentTimestamp)
        if (windowPoints.isEmpty()) return null

        // 1. Prefer hardware GPS Doppler speed when available and moving
        val validSpeedPoints = windowPoints.filter { (it.speed ?: 0f) >= 0.5f }
        if (validSpeedPoints.size >= 2) {
            var weightedSpeedSum = 0.0
            var totalWeight = 0.0
            validSpeedPoints.forEachIndexed { index, pt ->
                val weight = (index + 1).toDouble() // higher weight to newest points
                weightedSpeedSum += (pt.speed?.toDouble() ?: 0.0) * weight
                totalWeight += weight
            }
            if (totalWeight > 0) {
                val avgSpeedMps = weightedSpeedSum / totalWeight
                if (avgSpeedMps >= 0.5) {
                    return 1000.0 / avgSpeedMps
                }
            }
        }

        // 2. Fallback to Haversine coordinate distance over the window
        if (windowPoints.size < 2) return null

        val oldest = windowPoints.first()
        val newest = windowPoints.last()
        val durationSec = (newest.timestamp - oldest.timestamp) / 1000.0
        if (durationSec <= 0.0) return null

        var totalDistMeters = 0.0
        for (i in 1 until windowPoints.size) {
            val p1 = windowPoints[i - 1]
            val p2 = windowPoints[i]
            totalDistMeters += GeoUtils.haversineDistanceMeters(
                p1.latitude,
                p1.longitude,
                p2.latitude,
                p2.longitude
            )
        }

        if (totalDistMeters < minDistanceThresholdMeters) {
            return null
        }

        return durationSec / (totalDistMeters / 1000.0)
    }

    companion object {
        fun formatPace(paceSecPerKm: Double?): String {
            if (paceSecPerKm == null || paceSecPerKm <= 0 || paceSecPerKm.isInfinite() || paceSecPerKm.isNaN()) {
                return "--:--"
            }
            val totalSeconds = paceSecPerKm.toInt()
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }

        fun formatPaceSpeech(paceSecPerKm: Double?): String {
            if (paceSecPerKm == null || paceSecPerKm <= 0 || paceSecPerKm.isInfinite() || paceSecPerKm.isNaN()) {
                return "unknown"
            }
            val totalSeconds = paceSecPerKm.toInt()
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return when {
                seconds == 0 -> "$minutes"
                seconds < 10 -> "$minutes oh $seconds"
                else -> "$minutes $seconds"
            }
        }

        fun formatTime(totalSeconds: Long): String {
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                "%d:%02d:%02d".format(hours, minutes, seconds)
            } else {
                "%02d:%02d".format(minutes, seconds)
            }
        }
    }
}
