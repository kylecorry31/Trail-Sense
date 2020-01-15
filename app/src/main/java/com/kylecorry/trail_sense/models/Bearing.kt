package com.kylecorry.trail_sense.models

import com.kylecorry.trail_sense.navigator.normalizeAngle

class Bearing(_value: Float){
    val value: Float = normalizeAngle(_value)
}