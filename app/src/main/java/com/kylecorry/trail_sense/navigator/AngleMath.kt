package com.kylecorry.trail_sense.navigator

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