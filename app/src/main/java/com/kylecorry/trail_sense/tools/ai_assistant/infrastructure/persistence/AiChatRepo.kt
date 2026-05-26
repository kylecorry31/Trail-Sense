package com.kylecorry.trail_sense.tools.ai_assistant.infrastructure.persistence

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import java.util.UUID

class AiChatRepo private constructor(context: Context) {

    private val dao = AppDatabase.getInstance(context.applicationContext).aiChatDao()
    private val files = FileSubsystem.getInstance(context.applicationContext)

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
        deleteMessageImages(dao.getMessages(sessionId))
        dao.deleteMessagesBySession(sessionId)
        val session = dao.getSession(sessionId) ?: return
        dao.deleteSession(session)
    }

    suspend fun getMessages(sessionId: Long): List<ChatMessageEntity> =
        dao.getMessages(sessionId)

    suspend fun addMessage(
        sessionId: Long,
        text: String,
        isUser: Boolean,
        imagePath: String? = null
    ): Long {
        val msg = ChatMessageEntity(
            sessionId = sessionId,
            text = text,
            isUser = isUser,
            timestamp = System.currentTimeMillis(),
            imagePath = imagePath
        )
        val id = dao.insertMessage(msg)
        updateSessionTime(sessionId)
        return id
    }

    suspend fun saveMessageImage(image: Bitmap): String {
        val path = "$IMAGE_DIR/${UUID.randomUUID()}.webp"
        files.save(path, image, quality = IMAGE_QUALITY)
        return path
    }

    fun loadMessageImage(path: String): Bitmap? {
        return files.bitmap(path, Size(IMAGE_PREVIEW_SIZE, IMAGE_PREVIEW_SIZE))
    }

    suspend fun deleteAll() {
        deleteMessageImages(dao.getAllMessages())
        dao.deleteAllMessages()
        dao.deleteAllSessions()
    }

    private fun deleteMessageImages(messages: List<ChatMessageEntity>) {
        messages.mapNotNull { it.imagePath }.distinct().forEach {
            files.delete(it)
        }
    }

    companion object {
        private const val IMAGE_DIR = "ai_chat"
        private const val IMAGE_QUALITY = 85
        private const val IMAGE_PREVIEW_SIZE = 480
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
