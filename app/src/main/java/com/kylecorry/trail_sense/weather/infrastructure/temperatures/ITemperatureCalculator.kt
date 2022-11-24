package com.kylecorry.trail_sense.weather.infrastructure.temperatures

import com.kylecorry.sol.units.Temperature
import java.time.ZonedDateTime

interface ITemperatureCalculator {
    fun calculate(time: ZonedDateTime): Temperature
}