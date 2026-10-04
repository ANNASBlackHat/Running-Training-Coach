package com.runningcompanion.app.data.repository

import com.runningcompanion.app.data.db.dao.SessionDao
import com.runningcompanion.app.data.db.entity.SessionEntity
import com.runningcompanion.app.domain.model.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SessionRepository(
    private val sessionDao: SessionDao
) {
    fun getAllSessions(): Flow<List<Session>> {
        return sessionDao.getAllSessions().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getSessionById(id: String): Session? {
        return sessionDao.getSessionById(id)?.toDomain()
    }

    suspend fun saveSession(session: Session) {
        sessionDao.insertSession(SessionEntity.fromDomain(session))
    }

    suspend fun deleteSession(id: String) {
        sessionDao.deleteSession(id)
    }
}
