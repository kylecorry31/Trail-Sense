package com.kylecorry.trail_sense.weather.forcasting

import com.kylecorry.trail_sense.shared.PressureReading
import com.kylecorry.trail_sense.weather.PressureCharacteristic
import com.kylecorry.trail_sense.weather.PressureTendencyCalculator
import kotlin.math.abs

class HourlyForecaster : IWeatherForecaster {

    override fun forecast(readings: List<PressureReading>): Weather {

        val tendency = PressureTendencyCalculator.getPressureTendency(readings)

        val isStorm = tendency.amount <= STORM_THRESHOLD

        if (isStorm) {
            return Weather.Storm
        }

        val isFast = abs(tendency.amount) >= FAST_CHANGE

        return when (tendency.characteristic) {
            PressureCharacteristic.Falling -> if (isFast) Weather.WorseningFast else Weather.WorseningSlow
            PressureCharacteristic.Rising -> if (isFast) Weather.ImprovingFast else Weather.ImprovingSlow
            else -> Weather.NoChange
        }
    }

    companion object {
        private const val FAST_CHANGE = 2f
        private const val STORM_THRESHOLD = -6f
    }

}