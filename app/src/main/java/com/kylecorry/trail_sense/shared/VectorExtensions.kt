package com.kylecorry.trail_sense.shared

import com.kylecorry.trailsensecore.domain.math.Vector3
import com.kylecorry.trailsensecore.domain.math.toRadians
import kotlin.math.cos
import kotlin.math.sin

fun Vector3.rotate(angle: Float, axis: Int): Vector3 {
    return when (axis){
        0 -> {
            // Y and Z
            Vector3(x, cosDegrees(angle) * y - sinDegrees(angle) * z, sinDegrees(angle) * y + cosDegrees(angle) * z)
        }
        1 -> {
            // X and Z
            Vector3(cosDegrees(angle) * x - sinDegrees(angle) * z, y, sinDegrees(angle) * x + cosDegrees(angle) * z)
        }
        else -> {
            // X and Y
            Vector3(cosDegrees(angle) * x - sinDegrees(angle) * y, sinDegrees(angle) * x + cosDegrees(angle) * y, z)
        }
    }
}

fun cosDegrees(angle: Float): Float {
    return cos(angle.toRadians())
}

fun sinDegrees(angle: Float): Float {
    return sin(angle.toRadians())
}