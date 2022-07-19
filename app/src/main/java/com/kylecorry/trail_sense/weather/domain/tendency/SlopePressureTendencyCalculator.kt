package com.kylecorry.trail_sense.weather.domain.tendency

import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import java.time.Duration
import java.time.Instant
import kotlin.math.pow

class SlopePressureTendencyCalculator : BasePressureTendencyCalculator() {

    override fun getChangeAmount(readings: List<Reading<Pressure>>, duration: Duration): Float {
        val filtered = readings.filter { Duration.between(it.time, Instant.now()) <= duration }
        if (filtered.size < 2) return 0f
        return getSlope(
            filtered
        ) * 60 * 60 * 3
    }

    // TODO: Extract this to sol / use the version in sol
    private fun getSlope(readings: List<Reading<Pressure>>): Float {

        if (readings.isEmpty()) {
            return 0f
        }

        val startTime = readings.first().time.epochSecond
        val xBar = readings.map { it.time.epochSecond - startTime }.average().toFloat()
        val yBar = readings.map { it.value.hpa().pressure }.average().toFloat()

        var ssxx = 0.0f
        var ssxy = 0.0f
        var ssto = 0.0f

        for (i in readings.indices) {
            val x = (readings[i].time.epochSecond - startTime).toFloat()
            ssxx += (x - xBar).pow(2)
            ssxy += (x - xBar) * (readings[i].value.hpa().pressure - yBar)
            ssto += (readings[i].value.hpa().pressure - yBar).pow(2)
        }

        return ssxy / ssxx
    }

}