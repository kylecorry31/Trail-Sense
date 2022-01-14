package com.kylecorry.trail_sense.weather.infrastructure.persistence

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.database.IReadingRepo
import com.kylecorry.trail_sense.weather.domain.WeatherObservation
import java.time.Duration
import java.time.Instant

class WeatherRepo private constructor(context: Context) : IReadingRepo<WeatherObservation> {

    private val pressureRepo = PressureRepo.getInstance(context)

    override suspend fun add(reading: Reading<WeatherObservation>): Long {
        pressureRepo.addPressure(
            PressureReadingEntity(
                reading.value.pressure,
                reading.value.altitude,
                reading.value.altitudeError,
                reading.value.temperature,
                reading.value.humidity ?: 0f,
                reading.time.toEpochMilli()
            ).also {
                it.id = reading.value.id
            }
        )
        // TODO: Return the real value
        return reading.value.id
    }

    override suspend fun delete(reading: Reading<WeatherObservation>) {
        pressureRepo.deletePressure(
            PressureReadingEntity(
                reading.value.pressure,
                reading.value.altitude,
                reading.value.altitudeError,
                reading.value.temperature,
                reading.value.humidity ?: 0f,
                reading.time.toEpochMilli()
            ).also {
                it.id = reading.value.id
            }
        )
    }

    override suspend fun get(id: Long): Reading<WeatherObservation>? {
        val reading = pressureRepo.getPressure(id) ?: return null
        return Reading(
            WeatherObservation(
                reading.id,
                reading.pressure,
                reading.altitude,
                reading.temperature,
                reading.altitudeAccuracy,
                reading.humidity
            ), Instant.ofEpochMilli(reading.time)
        )
    }

    override suspend fun getAll(): List<Reading<WeatherObservation>> {
        return pressureRepo.getPressuresSync().map { reading ->
            Reading(
                WeatherObservation(
                    reading.id,
                    reading.pressure,
                    reading.altitude,
                    reading.temperature,
                    reading.altitudeAccuracy,
                    reading.humidity
                ), Instant.ofEpochMilli(reading.time)
            )
        }
    }

    override fun getAllLive(): LiveData<List<Reading<WeatherObservation>>> {
        return Transformations.map(pressureRepo.getPressures()) {
            it.map { reading ->
                Reading(
                    WeatherObservation(
                        reading.id,
                        reading.pressure,
                        reading.altitude,
                        reading.temperature,
                        reading.altitudeAccuracy,
                        reading.humidity
                    ), Instant.ofEpochMilli(reading.time)
                )
            }
        }
    }

    override suspend fun clean() {
        pressureRepo.deleteOlderThan(Instant.now().minus(PRESSURE_HISTORY_DURATION))
    }

    companion object {

        private val PRESSURE_HISTORY_DURATION = Duration.ofDays(2).plusHours(6)

        private var instance: WeatherRepo? = null

        @Synchronized
        fun getInstance(context: Context): WeatherRepo {
            if (instance == null) {
                instance = WeatherRepo(context.applicationContext)
            }
            return instance!!
        }
    }
}