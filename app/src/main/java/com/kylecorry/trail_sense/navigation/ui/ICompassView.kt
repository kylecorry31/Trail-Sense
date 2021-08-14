package com.kylecorry.trail_sense.navigation.ui

import androidx.annotation.ColorInt
import com.kylecorry.andromeda.core.units.Coordinate

interface ICompassView {
    fun setAzimuth(azimuth: Float)
    fun setLocation(location: Coordinate)
    fun setDeclination(declination: Float)
    fun setIndicators(indicators: List<BearingIndicator>)
    fun setDestination(bearing: Float?, @ColorInt color: Int? = null)
}