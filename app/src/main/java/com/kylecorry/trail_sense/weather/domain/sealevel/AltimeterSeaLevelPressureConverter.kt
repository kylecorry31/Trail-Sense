package com.kylecorry.trail_sense.weather.domain.sealevel

import com.kylecorry.trailsensecore.domain.weather.PressureAltitudeReading
import com.kylecorry.trailsensecore.domain.weather.PressureReading

internal class AltimeterSeaLevelPressureConverter(
    private val altitudeCalculator: IAltitudeCalculator,
    private val useTemperature: Boolean
) : ISeaLevelPressureConverter {

    override fun convert(readings: List<PressureAltitudeReading>): List<PressureReading> {
        val altitudeHistory = altitudeCalculator.convert(readings)

        return readings.mapIndexed { index, reading ->
            reading.copy(altitude = altitudeHistory[index].value).seaLevel(useTemperature)
        }
    }

}