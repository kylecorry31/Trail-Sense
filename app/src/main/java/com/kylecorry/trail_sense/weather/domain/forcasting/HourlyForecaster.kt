package com.kylecorry.trail_sense.weather.domain.forcasting

import com.kylecorry.trail_sense.weather.domain.PressureReading
import com.kylecorry.trail_sense.weather.domain.tendency.DropPressureTendencyCalculator
import com.kylecorry.trail_sense.weather.domain.tendency.PressureCharacteristic
import java.time.Duration
import kotlin.math.abs

class HourlyForecaster(private val stormThreshold: Float, private val changeThreshold: Float) : IWeatherForecaster {

    override fun forecast(readings: List<PressureReading>): Weather {

        val tendency = DropPressureTendencyCalculator().calculate(readings, Duration.ofHours(3))

        val isStorm = tendency.amount <= stormThreshold

        if (isStorm) {
            return Weather.Storm
        }

        val isSteady = abs(tendency.amount) < changeThreshold
        if (isSteady){
            return Weather.NoChange
        }

        val isFast = abs(tendency.amount) >= changeThreshold + 2

        return when (tendency.characteristic) {
            PressureCharacteristic.Falling -> if (isFast) Weather.WorseningFast else Weather.WorseningSlow
            PressureCharacteristic.Rising -> if (isFast) Weather.ImprovingFast else Weather.ImprovingSlow
            else -> Weather.NoChange
        }
    }
}