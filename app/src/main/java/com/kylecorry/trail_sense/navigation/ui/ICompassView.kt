package com.kylecorry.trail_sense.navigation.ui

import com.kylecorry.trailsensecore.domain.geo.Bearing

interface ICompassView {
    fun setAzimuth(azimuth: Bearing)
    fun setIndicators(indicators: List<BearingIndicator>)

}