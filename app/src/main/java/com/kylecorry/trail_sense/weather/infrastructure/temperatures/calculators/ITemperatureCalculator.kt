package com.kylecorry.trail_sense.weather.infrastructure.temperatures.calculators

import com.kylecorry.sol.units.Temperature
import java.time.ZonedDateTime

internal interface ITemperatureCalculator {
    suspend fun calculate(time: ZonedDateTime): Temperature
}