package com.kylecorry.trail_sense.weather.domain

import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.units.Pressure

fun Pressure.isHigh(): Boolean {
    return Meteorology.isHighPressure(this)
}

fun Pressure.isLow(): Boolean {
    return Meteorology.isLowPressure(this)
}