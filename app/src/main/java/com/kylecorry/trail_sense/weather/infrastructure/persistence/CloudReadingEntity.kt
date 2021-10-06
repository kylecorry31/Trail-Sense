package com.kylecorry.trail_sense.weather.infrastructure.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "clouds")
data class CloudReadingEntity(
    @ColumnInfo(name = "time") val time: Instant,
    @ColumnInfo(name = "cover") val cover: Float
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0
}