package com.kylecorry.trail_sense.weather.forcasting

import com.kylecorry.trail_sense.models.PressureTendency

interface IWeatherForecaster {

    fun forcast(pressure: Float, tendency: PressureTendency): String

}