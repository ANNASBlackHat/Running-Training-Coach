package com.runningcompanion.app.data.repository

import com.runningcompanion.app.data.db.dao.WorkoutDao
import com.runningcompanion.app.data.db.entity.WorkoutEntity
import com.runningcompanion.app.domain.model.Workout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkoutRepository(
    private val workoutDao: WorkoutDao
) {
    fun getAllWorkouts(): Flow<List<Workout>> {
        return workoutDao.getAllWorkouts().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getWorkoutById(id: String): Workout? {
        return workoutDao.getWorkoutById(id)?.toDomain()
    }

    suspend fun saveWorkout(workout: Workout) {
        workoutDao.insertWorkout(WorkoutEntity.fromDomain(workout))
    }

    suspend fun deleteWorkout(id: String) {
        workoutDao.deleteWorkout(id)
    }
}
