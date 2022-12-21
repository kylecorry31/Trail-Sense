package com.kylecorry.trail_sense.shared.sensors.thermometer

import com.kylecorry.sol.units.Temperature

interface ITemperatureCalibrator {
    fun calibrate(temperature: Temperature): Temperature
}