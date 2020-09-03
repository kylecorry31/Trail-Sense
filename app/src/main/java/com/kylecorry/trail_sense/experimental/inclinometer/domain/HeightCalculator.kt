package com.kylecorry.trail_sense.experimental.inclinometer.domain

import kotlin.math.tan

class HeightCalculator {

    fun calculate(distance: Float, inclination: Float, phoneHeight: Float): Float {

        if (inclination < 0 || inclination == 90f) {
            return 0f
        }

        val heightFromPhone = tan(inclination) * distance

        return heightFromPhone + phoneHeight
    }

}