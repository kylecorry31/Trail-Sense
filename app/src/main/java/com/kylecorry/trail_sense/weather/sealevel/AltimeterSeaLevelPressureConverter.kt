package com.kylecorry.trail_sense.weather.sealevel

import com.kylecorry.trail_sense.weather.altimeter.IAltitudeCalculator
import com.kylecorry.trail_sense.shared.PressureAltitudeReading
import com.kylecorry.trail_sense.shared.PressureReading

class AltimeterSeaLevelPressureConverter(private val altitudeCalculator: IAltitudeCalculator) :
    ISeaLevelPressureConverter {

    override fun convert(readings: List<PressureAltitudeReading>): List<PressureReading> {
        val altitudeHistory = altitudeCalculator.convert(readings)

        return readings.mapIndexed { index, reading ->
            PressureReading(
                reading.time,
                SeaLevelPressureCalibrator.calibrate(
                    reading.pressure,
                    altitudeHistory[index].value
                )
            )
        }
    }

}