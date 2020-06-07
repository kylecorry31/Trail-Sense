package com.kylecorry.trail_sense.shared.domain

import kotlin.math.*

/**
 * An angle in degrees
 */
data class Angle(val degrees: Float) {

    val unitDegrees: Float
        get() {
            var outputAngle = degrees
            while (outputAngle < 0) outputAngle += 360
            return outputAngle % 360
        }

    val radians: Float = Math.toRadians(degrees.toDouble()).toFloat()

    val unitRadians: Float
        get() = Math.toRadians(unitDegrees.toDouble()).toFloat()

    fun sin(): Float {
        return sin(radians)
    }

    fun cos(): Float {
        return cos(radians)
    }

    fun tan(): Float {
        return tan(radians)
    }

    operator fun minus(other: Angle): Angle {
        var delta = other.degrees - degrees
        delta += 180
        delta -= floor(delta / 360) * 360
        delta -= 180
        if (abs(abs(delta) - 180) <= Float.MIN_VALUE) {
            delta = 180f
        }
        return Angle(delta)
    }

    companion object {
        fun fromRadians(radians: Float): Angle {
            return Angle(Math.toDegrees(radians.toDouble()).toFloat())
        }

        fun atan2(y: Float, x: Float): Angle {
            return fromRadians(kotlin.math.atan2(y, x))
        }

        fun atan(value: Float): Angle {
            return fromRadians(kotlin.math.atan(value))
        }

        fun asin(value: Float): Angle {
            return fromRadians(kotlin.math.asin(value))
        }

        fun acos(value: Float): Angle {
            return fromRadians(kotlin.math.acos(value))
        }
    }

}