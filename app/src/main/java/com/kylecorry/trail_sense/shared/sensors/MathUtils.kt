package com.kylecorry.trail_sense.shared.sensors

object MathUtils {
    fun wrap(value: Float, min: Float, max: Float): Float {
        val range = max - min

        var newValue = value

        while (newValue > max) {
            newValue -= range
        }

        while (newValue < min) {
            newValue += range
        }

        return newValue
    }
}