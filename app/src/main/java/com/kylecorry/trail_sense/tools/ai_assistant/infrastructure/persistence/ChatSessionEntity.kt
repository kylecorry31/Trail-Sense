package com.kylecorry.trail_sense.tools.ai_assistant.infrastructure.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_chat_sessions")
data class ChatSessionEntity(
    @ColumnInfo(name = "title") val title: String?,
    @ColumnInfo(name = "created_on") val createdOn: Long,
    @ColumnInfo(name = "updated_on") val updatedOn: Long
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0
}
