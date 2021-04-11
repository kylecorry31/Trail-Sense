package com.kylecorry.trail_sense.tools.battery.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "battery")
class BatteryReadingEntity(
    @ColumnInfo(name = "percent") val percent: Float,
    @ColumnInfo(name = "isCharging") val isCharging: Boolean,
    @ColumnInfo(name = "time") val time: Instant
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0
}