package com.kylecorry.trail_sense.weather.infrastructure.temperatures

import com.kylecorry.sol.math.SolMath.square
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import java.time.Instant
import java.time.ZonedDateTime

class QuadraticTemperatureCalculator(
    private val low: Reading<Temperature>,
    high: Reading<Temperature>
) : ITemperatureCalculator {

    private val b = low.value.celsius().temperature
    private val a = (high.value.celsius().temperature - b) / square(getX(high.time))

    override fun calculate(time: ZonedDateTime): Temperature {
        val x = getX(time.toInstant())
        return Temperature.celsius(a * square(x) + b)
    }

    private fun getX(time: Instant): Float {
        return Time.hoursBetween(low.time, time)
    }
}