package com.kylecorry.trail_sense.navigation.domain.compass

import com.kylecorry.trail_sense.shared.math.normalizeAngle

class Bearing(_value: Float){
    val value: Float =
        normalizeAngle(_value)
}