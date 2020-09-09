package com.kylecorry.trail_sense.weather.domain.sealevel

import com.kylecorry.trail_sense.weather.domain.PressureAltitudeReading
import com.kylecorry.trail_sense.weather.domain.PressureReading

internal class AltimeterSeaLevelPressureConverter(private val altitudeCalculator: IAltitudeCalculator) :
    ISeaLevelPressureConverter {

    override fun convert(readings: List<PressureAltitudeReading>): List<PressureReading> {
        val altitudeHistory = altitudeCalculator.convert(readings)

        return readings.mapIndexed { index, reading ->
            PressureReading(
                reading.time,
                SeaLevelPressureCalibrator.calibrate(
                    reading.pressure,
                    altitudeHistory[index].value,
                    if (reading.temperature.isNaN()) 16f else reading.temperature
                )
            )
        }
    }

}