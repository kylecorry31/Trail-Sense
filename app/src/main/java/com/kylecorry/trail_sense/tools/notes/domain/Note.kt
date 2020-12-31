package com.kylecorry.trail_sense.tools.notes.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes"
)
data class Note(
    @ColumnInfo(name = "title") val title: String?,
    @ColumnInfo(name = "contents") val contents: String?,
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0
}