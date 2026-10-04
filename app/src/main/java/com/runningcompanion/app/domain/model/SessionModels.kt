package com.runningcompanion.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class GpsPoint(
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val altitude: Double? = null,
    val speed: Float? = null
)

@Serializable
data class KmSplit(
    val km: Int,
    val paceSecPerKm: Double
)

@Serializable
data class SegmentResult(
    val index: Int,
    val type: SegmentType,
    val setNumber: Int?,
    val startedAt: Long,
    val endedAt: Long,
    val durationSec: Long,
    val distanceM: Double,
    val avgPaceSecPerKm: Double?,
    val kmSplits: List<KmSplit> = emptyList(),
    val elevationGainM: Double? = null
)

@Serializable
data class SessionTotals(
    val durationSec: Long,
    val distanceM: Double,
    val avgPaceSecPerKm: Double?
)

@Serializable
data class Session(
    val id: String,
    val workoutId: String,
    val workoutSnapshot: Workout,
    val startedAt: Long,
    val endedAt: Long,
    val results: List<SegmentResult>,
    val totals: SessionTotals,
    val track: List<GpsPoint>
)
