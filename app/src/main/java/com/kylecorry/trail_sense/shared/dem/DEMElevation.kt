package com.kylecorry.trail_sense.shared.dem

data class DEMElevation(private val _elevation: Float, val waterType: DEMElevationType?) {
    val elevation: Float
        get() = if (waterType == DEMElevationType.Ocean) {
            0f
        } else {
            _elevation
        }
}
