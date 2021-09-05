package com.kylecorry.trail_sense.weather.domain.forcasting

import com.kylecorry.sol.science.meteorology.Weather
import com.kylecorry.trail_sense.weather.domain.PressureReading

interface IWeatherForecaster {

    fun forecast(readings: List<PressureReading>): Weather

}