package com.kylecorry.trail_sense.shared.math

import com.kylecorry.trail_sense.shared.toRadians
import kotlin.math.cos
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