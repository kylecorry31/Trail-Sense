package com.kylecorry.trail_sense.weather.domain.forcasting

import com.kylecorry.trailsensecore.domain.weather.PressureReading
import com.kylecorry.trailsensecore.domain.weather.Weather

interface IWeatherForecaster {

    fun forecast(readings: List<PressureReading>): Weather

}