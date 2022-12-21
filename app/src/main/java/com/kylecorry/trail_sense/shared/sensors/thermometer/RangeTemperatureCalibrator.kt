package com.kylecorry.trail_sense.shared.sensors.thermometer

import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.units.Temperature

class RangeTemperatureCalibrator(
    private val sensorMinC: Float,
    private val sensorMaxC: Float,
    private val calibratedMinC: Float,
    private val calibratedMaxC: Float
) : ITemperatureCalibrator {
    override fun calibrate(temperature: Temperature): Temperature {
        return Temperature.celsius(
            SolMath.map(
                temperature.celsius().temperature,
                sensorMinC,
                sensorMaxC,
                calibratedMinC,
                calibratedMaxC
            )
        ).convertTo(temperature.units)
    }
}