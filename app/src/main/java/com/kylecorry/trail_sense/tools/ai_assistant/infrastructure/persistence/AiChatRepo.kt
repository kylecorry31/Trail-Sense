package com.kylecorry.trail_sense.tools.ai_assistant.infrastructure.persistence

import android.content.Context
import com.kylecorry.trail_sense.main.persistence.AppDatabase

class AiChatRepo private constructor(context: Context) {

    private val dao = AppDatabase.getInstance(context.applicationContext).aiChatDao()

    suspend fun getAllSessions(): List<ChatSessionEntity> = dao.getAllSessions()

    suspend fun getSession(id: Long): ChatSessionEntity? = dao.getSession(id)

    suspend fun createSession(title: String?): Long {
        val now = System.currentTimeMillis()
        val session = ChatSessionEntity(title = title, createdOn = now, updatedOn = now)
        return dao.insertSession(session)
    }

    suspend fun updateSessionTime(sessionId: Long) {
        val session = dao.getSession(sessionId) ?: return
        val updated = session.copy(updatedOn = System.currentTimeMillis())
        updated.id = session.id
        dao.updateSession(updated)
    }

    suspend fun deleteSession(sessionId: Long) {
        dao.deleteMessagesBySession(sessionId)
        val session = dao.getSession(sessionId) ?: return
        dao.deleteSession(session)
    }

    suspend fun getMessages(sessionId: Long): List<ChatMessageEntity> =
        dao.getMessages(sessionId)

    suspend fun addMessage(sessionId: Long, text: String, isUser: Boolean): Long {
        val msg = ChatMessageEntity(
            sessionId = sessionId,
            text = text,
            isUser = isUser,
            timestamp = System.currentTimeMillis()
        )
        val id = dao.insertMessage(msg)
        updateSessionTime(sessionId)
        return id
    }

    suspend fun deleteAll() {
        dao.deleteAllMessages()
        dao.deleteAllSessions()
    }

    companion object {
        private var instance: AiChatRepo? = null

        @Synchronized
        fun getInstance(context: Context): AiChatRepo {
            if (instance == null) {
                instance = AiChatRepo(context.applicationContext)
            }
            return instance!!
        }
    }
}
