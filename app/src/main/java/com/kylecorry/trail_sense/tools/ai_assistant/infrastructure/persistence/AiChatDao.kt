package com.kylecorry.trail_sense.tools.ai_assistant.infrastructure.persistence

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface AiChatDao {
    @Query("SELECT * FROM ai_chat_sessions ORDER BY updated_on DESC")
    suspend fun getAllSessions(): List<ChatSessionEntity>

    @Query("SELECT * FROM ai_chat_sessions WHERE _id = :id LIMIT 1")
    suspend fun getSession(id: Long): ChatSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity): Long

    @Update
    suspend fun updateSession(session: ChatSessionEntity)

    @Delete
    suspend fun deleteSession(session: ChatSessionEntity)

    @Query("DELETE FROM ai_chat_messages WHERE session_id = :sessionId")
    suspend fun deleteMessagesBySession(sessionId: Long)

    @Query("SELECT * FROM ai_chat_messages WHERE session_id = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessages(sessionId: Long): List<ChatMessageEntity>

    @Query("SELECT * FROM ai_chat_messages WHERE _id = :id LIMIT 1")
    suspend fun getMessage(id: Long): ChatMessageEntity?

    @Query("SELECT * FROM ai_chat_messages")
    suspend fun getAllMessages(): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM ai_chat_messages WHERE _id = :id")
    suspend fun deleteMessage(id: Long)

    @Query("DELETE FROM ai_chat_sessions")
    suspend fun deleteAllSessions()

    @Query("DELETE FROM ai_chat_messages")
    suspend fun deleteAllMessages()
}
