package com.kylecorry.trail_sense.weather.infrastructure.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.weather.domain.RawWeatherObservation
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

    fun toWeatherObservation(): Reading<RawWeatherObservation> {
        return Reading(
            RawWeatherObservation(
                id,
                pressure,
                altitude,
                temperature,
                altitudeAccuracy,
                humidity
            ),
            Instant.ofEpochMilli(time)
        )
    }

    companion object {
        fun from(reading: Reading<RawWeatherObservation>): PressureReadingEntity {
            return PressureReadingEntity(
                reading.value.pressure,
                reading.value.altitude,
                reading.value.altitudeError,
                reading.value.temperature,
                reading.value.humidity ?: 0f,
                reading.time.toEpochMilli()
            ).also {
                it.id = reading.value.id
            }
        }
    }

}