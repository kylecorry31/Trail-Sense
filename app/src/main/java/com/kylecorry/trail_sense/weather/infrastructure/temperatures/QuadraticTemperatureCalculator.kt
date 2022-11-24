package com.kylecorry.trail_sense.weather.infrastructure.temperatures

import com.kylecorry.sol.math.SolMath.square
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import java.time.Instant
import java.time.ZonedDateTime

class QuadraticTemperatureCalculator(
    private val first: Reading<Temperature>,
    private val second: Reading<Temperature>
) : ITemperatureCalculator {

    private val b = first.value.celsius().temperature
    private val a = (second.value.celsius().temperature - b) / getX(second.time)

    override fun calculate(time: ZonedDateTime): Temperature {
        val x = getX(time.toInstant())
        return Temperature.celsius(a * square(x) + b)
    }

    private fun getX(time: Instant): Float {
        return Time.hoursBetween(first.time, time)
    }
}