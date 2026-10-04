package com.runningcompanion.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.runningcompanion.app.domain.model.Workout
import kotlinx.serialization.json.Json

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isTemplate: Boolean,
    val workoutJson: String
) {
    fun toDomain(): Workout {
        return Json.decodeFromString(Workout.serializer(), workoutJson)
    }

    companion object {
        fun fromDomain(workout: Workout): WorkoutEntity {
            return WorkoutEntity(
                id = workout.id,
                name = workout.name,
                isTemplate = workout.isTemplate,
                workoutJson = Json.encodeToString(Workout.serializer(), workout)
            )
        }
    }
}
