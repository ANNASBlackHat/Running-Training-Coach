package com.runningcompanion.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.runningcompanion.app.data.db.entity.WorkoutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts ORDER BY isTemplate DESC, name ASC")
    fun getAllWorkouts(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getWorkoutById(id: String): WorkoutEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWorkouts(workouts: List<WorkoutEntity>)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkout(id: String)
}
