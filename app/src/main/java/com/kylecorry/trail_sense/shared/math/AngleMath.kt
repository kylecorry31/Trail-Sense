package com.kylecorry.trail_sense.shared.math

import kotlin.math.*

fun deltaAngle(angle1: Float, angle2: Float): Float {
    var delta = angle2 - angle1
    delta += 180
    delta -= floor(delta / 360) * 360
    delta -= 180
    if (abs(abs(delta) - 180) <= Float.MIN_VALUE) {
        delta = 180f
    }
    return delta
}