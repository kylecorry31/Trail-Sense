package com.kylecorry.trail_sense.weather.infrastructure.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "pressures"
)
data class PressureReadingEntity(
    @ColumnInfo(name = "pressure") val pressure: Float,
    @ColumnInfo(name = "altitude") val altitude: Float,
    @ColumnInfo(name = "altitude_accuracy") val altitudeAccuracy: Float?,
    @ColumnInfo(name = "temperature") val temperature: Float,
    @ColumnInfo(name = "humidity") val humidity: Float,
    @ColumnInfo(name = "time") val time: Long
) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0

}