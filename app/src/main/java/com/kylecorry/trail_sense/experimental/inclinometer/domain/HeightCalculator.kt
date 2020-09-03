package com.kylecorry.trail_sense.experimental.inclinometer.domain

import com.kylecorry.trail_sense.shared.math.tanDegrees

class HeightCalculator {

    fun calculate(distance: Float, inclination: Float, phoneHeight: Float): Float {

        if (inclination < 0 || inclination == 90f) {
            return 0f
        }

        val heightFromPhone = tanDegrees(inclination) * distance

        return heightFromPhone + phoneHeight
    }

}