package com.kylecorry.trail_sense.shared.sensors

import com.kylecorry.trail_sense.navigation.domain.compass.Bearing

interface ICompass: ISensor {
    val bearing: Bearing
    var declination: Float

    fun setSmoothing(smoothing: Int)
}