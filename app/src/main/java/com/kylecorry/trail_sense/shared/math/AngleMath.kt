package com.kylecorry.trail_sense.shared.math

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Converts an angle to between 0 and 360
 * @param angle the angle in degrees
 * @return the normalized angle
 */
fun normalizeAngle(angle: Float): Float {
    var outputAngle = angle
    while (outputAngle < 0) outputAngle += 360
    return outputAngle % 360
}

fun normalizeAngle(angle: Double): Double {
    var outputAngle = angle
    while (outputAngle < 0) outputAngle += 360
    return outputAngle % 360
}

fun sinDegrees(angle: Double): Double {
    return sin(angle.toRadians())
}

fun cosDegrees(angle: Double): Double {
    return cos(angle.toRadians())
}

fun Double.toRadians(): Double {
    return Math.toRadians(this)
}

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