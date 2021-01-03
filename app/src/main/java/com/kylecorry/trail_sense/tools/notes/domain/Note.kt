package com.kylecorry.trail_sense.tools.notes.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "notes"
)
data class Note(
    @ColumnInfo(name = "title") val title: String?,
    @ColumnInfo(name = "contents") val contents: String?,
    @ColumnInfo(name = "created") val createdOn: Long
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0

    val createdInstant: Instant
        get() = Instant.ofEpochMilli(createdOn)
}