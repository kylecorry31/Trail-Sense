package com.kylecorry.trail_sense.weather.domain.sealevel

import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.weather.domain.PressureReading
import com.kylecorry.trail_sense.weather.domain.RawWeatherObservation

class SimpleSeaLevelCalibrationStrategy(private val useTemperature: Boolean) :
    ISeaLevelCalibrationStrategy {
    override fun calibrate(readings: List<Reading<RawWeatherObservation>>): List<PressureReading> {
        return readings.map {
            PressureReading(
                it.time,
                it.value.seaLevel(useTemperature).pressure
            )
        }
    }
}