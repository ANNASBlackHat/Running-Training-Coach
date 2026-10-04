package com.runningcompanion.app.di

import android.content.Context
import com.runningcompanion.app.data.db.AppDatabase
import com.runningcompanion.app.data.repository.SessionRepository
import com.runningcompanion.app.data.repository.WorkoutRepository

class AppContainer(context: Context) {
    private val database = AppDatabase.getInstance(context)

    val workoutRepository: WorkoutRepository by lazy {
        WorkoutRepository(database.workoutDao())
    }

    val sessionRepository: SessionRepository by lazy {
        SessionRepository(database.sessionDao())
    }
}
