package com.runningcompanion.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.runningcompanion.app.domain.model.Session
import kotlinx.serialization.json.Json

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val workoutName: String,
    val startedAt: Long,
    val endedAt: Long,
    val totalDistanceM: Double,
    val totalDurationSec: Long,
    val avgPaceSecPerKm: Double?,
    val sessionJson: String
) {
    fun toDomain(): Session {
        return Json.decodeFromString(Session.serializer(), sessionJson)
    }

    companion object {
        fun fromDomain(session: Session): SessionEntity {
            return SessionEntity(
                id = session.id,
                workoutId = session.workoutId,
                workoutName = session.workoutSnapshot.name,
                startedAt = session.startedAt,
                endedAt = session.endedAt,
                totalDistanceM = session.totals.distanceM,
                totalDurationSec = session.totals.durationSec,
                avgPaceSecPerKm = session.totals.avgPaceSecPerKm,
                sessionJson = Json.encodeToString(Session.serializer(), session)
            )
        }
    }
}
