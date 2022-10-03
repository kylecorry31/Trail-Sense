package com.kylecorry.trail_sense.weather.domain.sealevel.loess

import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.data.DataUtils
import com.kylecorry.trail_sense.weather.domain.RawWeatherObservation

class LoessSeaLevelPressureConverter(
    private val altitudeSmoothing: Float = 0.3f,
    private val pressureSmoothing: Float = 0.1f
) {

    fun convert(
        readings: List<Reading<RawWeatherObservation>>,
        factorInTemperature: Boolean
    ): List<Reading<Pressure>> {
//        val smoothed = DataUtils.smoothTemporal(
//            readings,
//            altitudeSmoothing,
//            { it.altitude }
//        ) { reading, value ->
//            reading.copy(altitude = value)
//        }

        val smoothed = DataUtils.smoothGeospatial(
            readings,
            altitudeSmoothing,
            DataUtils.GeospatialSmoothingType.FromStart,
            { it.value.location },
            { it.value.altitude }
        ) { reading, smoothedValue ->
            reading.copy(value = reading.value.copy(altitude = smoothedValue))
        }

        val seaLevel = smoothed.map {
            Reading(it.value.seaLevel(factorInTemperature), it.time)
        }

        return DataUtils.smoothTemporal(
            seaLevel,
            pressureSmoothing,
            { it.pressure }
        ) { reading, value ->
            reading.copy(pressure = value)
        }
    }
}