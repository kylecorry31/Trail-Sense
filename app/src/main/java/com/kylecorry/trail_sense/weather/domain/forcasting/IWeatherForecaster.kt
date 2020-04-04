package com.kylecorry.trail_sense.weather.domain.forcasting

import com.kylecorry.trail_sense.shared.PressureReading

interface IWeatherForecaster {

    fun forecast(readings: List<PressureReading>): Weather

}