package com.kylecorry.trail_sense.weather.domain.sealevel.loess

import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.weather.domain.RawWeatherObservation
import com.kylecorry.trail_sense.weather.domain.sealevel.ISeaLevelCalibrationStrategy

class LoessSeaLevelCalibrationStrategy(
    private val elevationSmoothing: Float,
    private val pressureSmoothing: Float,
    private val useTemperature: Boolean,
    private val usePathSmoothing: Boolean
) :
    ISeaLevelCalibrationStrategy {
    override fun calibrate(readings: List<Reading<RawWeatherObservation>>): List<Reading<Pressure>> {
        return LoessSeaLevelPressureConverter(
            elevationSmoothing,
            pressureSmoothing,
            usePathSmoothing
        ).convert(readings, useTemperature)
    }
}