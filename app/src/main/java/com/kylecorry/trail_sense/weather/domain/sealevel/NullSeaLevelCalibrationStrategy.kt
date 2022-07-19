package com.kylecorry.trail_sense.weather.domain.sealevel

import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.weather.domain.RawWeatherObservation

class NullSeaLevelCalibrationStrategy : ISeaLevelCalibrationStrategy {
    override fun calibrate(readings: List<Reading<RawWeatherObservation>>): List<Reading<Pressure>> {
        return readings.map { Reading(Pressure.hpa(it.value.pressure), it.time) }
    }
}