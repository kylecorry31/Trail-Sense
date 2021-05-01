package com.kylecorry.trail_sense.weather.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kylecorry.trailsensecore.domain.weather.PressureAltitudeReading
import java.time.Instant

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

    fun toPressureAltitudeReading(): PressureAltitudeReading {
        return PressureAltitudeReading(Instant.ofEpochMilli(time), pressure, altitude, temperature, altitudeAccuracy)
    }

    companion object {
        fun from(pressure: PressureAltitudeReading): PressureReadingEntity {
            return PressureReadingEntity(pressure.pressure, pressure.altitude, pressure.altitudeError, pressure.temperature, 0f, pressure.time.toEpochMilli())
        }
    }

}