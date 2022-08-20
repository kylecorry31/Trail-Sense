package com.kylecorry.trail_sense.weather.domain.sealevel.loess

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.data.DataUtils
import com.kylecorry.trail_sense.weather.domain.RawWeatherObservation
import java.time.Duration
import java.time.Instant

class LoessSeaLevelPressureConverter(
    private val altitudeSmoothing: Float = 0.3f,
    private val pressureSmoothing: Float = 0.1f
) {

    fun convert(
        readings: List<Reading<RawWeatherObservation>>,
        factorInTemperature: Boolean
    ): List<Reading<Pressure>> {

        val start = readings.firstOrNull()?.time ?: Instant.now()

        var smoothed = readings

        smoothed = DataUtils.smooth(
            smoothed,
            altitudeSmoothing,
            { _, value ->
                Vector2(
                    Duration.between(start, value.time).toMillis() / 1000f,
                    value.value.altitude
                )
            }
        ) { reading, smoothedValue -> reading.copy(value = reading.value.copy(altitude = smoothedValue.y)) }

        smoothed = DataUtils.smooth(
            smoothed,
            pressureSmoothing,
            { _, value ->
                Vector2(
                    Duration.between(start, value.time).toMillis() / 1000f,
                    value.value.pressure
                )
            }
        ) { reading, smoothedValue -> reading.copy(value = reading.value.copy(pressure = smoothedValue.y)) }

        return smoothed.map {
            Reading(it.value.seaLevel(factorInTemperature), it.time)
        }

    }
}