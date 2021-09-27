package com.kylecorry.trail_sense.weather.infrastructure.clouds

import android.graphics.Color
import com.kylecorry.andromeda.core.specifications.Specification
import kotlin.math.max
import kotlin.math.min

class SaturationIsObstacleSpecification(private val threshold: Float) : Specification<Int>() {
    override fun isSatisfiedBy(value: Int): Boolean {
        val blue = Color.blue(value)
        val red = Color.red(value)
        val green = Color.green(value)

        val min = min(red, min(blue, green))
        val max = max(red, max(blue, green))

        val sat = 1 - min / max.toFloat().coerceAtLeast(1f)

        return sat > threshold
    }
}