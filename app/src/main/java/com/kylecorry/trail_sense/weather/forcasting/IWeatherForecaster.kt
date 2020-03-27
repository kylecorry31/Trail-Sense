package com.kylecorry.trail_sense.weather.forcasting

import com.kylecorry.trail_sense.models.PressureReading

interface IWeatherForecaster {

    fun forecast(readings: List<PressureReading>): Weather

}