package com.kylecorry.trail_sense.weather.forcasting

import com.kylecorry.trail_sense.models.PressureReading
import kotlin.math.pow

class DailyForecaster : IWeatherForecaster {

    override fun forecast(readings: List<PressureReading>): Weather {

        val slope = getSlope(readings) * 60 * 60 * 3

        return when {
            slope <= -CHANGE_THRESHOLD -> Weather.WorseningSlow
            slope >= CHANGE_THRESHOLD -> Weather.ImprovingSlow
            else -> Weather.Unknown
        }

    }

    private fun getSlope(readings: List<PressureReading>): Float {
        val startTime = readings.first().time.epochSecond
        val xBar = readings.map { it.time.epochSecond - startTime }.average().toFloat()
        val yBar = readings.map { it.value }.average().toFloat()

        var ssxx = 0.0f
        var ssxy = 0.0f
        var ssto = 0.0f

        for (i in readings.indices) {
            val x = (readings[i].time.epochSecond - startTime).toFloat()
            ssxx += (x - xBar).pow(2)
            ssxy += (x - xBar) * (readings[i].value - yBar)
            ssto += (readings[i].value - yBar).pow(2)
        }

        val b1 = ssxy / ssxx

        return b1
    }

    companion object {
        private const val CHANGE_THRESHOLD = 0.5f
    }

}