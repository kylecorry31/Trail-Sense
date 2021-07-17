package com.kylecorry.trail_sense.tools.packs.infrastructure

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "packs")
data class PackEntity(
    @ColumnInfo(name = "name") val name: String
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0
}