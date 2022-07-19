package com.kylecorry.trail_sense.weather.domain.sealevel

import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.weather.domain.RawWeatherObservation

interface ISeaLevelCalibrationStrategy {

    fun calibrate(readings: List<Reading<RawWeatherObservation>>): List<Reading<Pressure>>

}