package com.kylecorry.trail_sense.shared.dem

data class DEMElevation(private val _elevation: Float, val isOcean: Boolean?) {
    val elevation: Float
        get() = if (isOcean == true) {
            0f
        } else {
            _elevation
        }
}
