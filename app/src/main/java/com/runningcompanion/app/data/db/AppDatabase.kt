package com.runningcompanion.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.runningcompanion.app.data.db.dao.SessionDao
import com.runningcompanion.app.data.db.dao.WorkoutDao
import com.runningcompanion.app.data.db.entity.SessionEntity
import com.runningcompanion.app.data.db.entity.WorkoutEntity
import com.runningcompanion.app.domain.model.WorkoutTemplates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [WorkoutEntity::class, SessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "running_companion.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default templates
                        CoroutineScope(Dispatchers.IO).launch {
                            val templates = WorkoutTemplates.allTemplates.map {
                                WorkoutEntity.fromDomain(it)
                            }
                            getInstance(context).workoutDao().insertWorkouts(templates)
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
