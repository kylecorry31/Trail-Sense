package com.kylecorry.trail_sense.weather.domain

import com.kylecorry.sol.science.meteorology.WeatherService
import com.kylecorry.sol.units.Pressure

fun Pressure.isHigh(): Boolean {
    return WeatherService().isHighPressure(this)
}

fun Pressure.isLow(): Boolean {
    return WeatherService().isLowPressure(this)
}