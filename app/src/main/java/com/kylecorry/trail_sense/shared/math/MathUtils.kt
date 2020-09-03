package com.kylecorry.trail_sense.shared.math

import kotlin.math.max
import kotlin.math.min

object MathUtils {

    fun clamp(value: Float, minimum: Float, maximum: Float): Float {
        return min(maximum, max(minimum, value))
    }

}