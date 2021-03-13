package com.kylecorry.trail_sense.navigation.ui

import androidx.annotation.ColorInt
import com.kylecorry.trailsensecore.domain.geo.Bearing

interface ICompassView {
    fun setAzimuth(azimuth: Bearing)
    fun setIndicators(indicators: List<BearingIndicator>)
    fun setDestination(bearing: Bearing?, @ColorInt color: Int? = null)
}