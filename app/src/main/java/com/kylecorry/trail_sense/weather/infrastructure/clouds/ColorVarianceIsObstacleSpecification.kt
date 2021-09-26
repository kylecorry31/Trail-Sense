package com.kylecorry.trail_sense.weather.infrastructure.clouds

import android.graphics.Color
import com.kylecorry.andromeda.core.specifications.Specification
import kotlin.math.abs

class ColorVarianceIsObstacleSpecification(private val threshold: Int) : Specification<Int>() {
    override fun isSatisfiedBy(value: Int): Boolean {
        val blue = Color.blue(value)
        val green = Color.green(value)
        val red = Color.red(value)
        val average = (red + blue + green) / 3f
        val diff = abs(red - average) + abs(blue - average) + abs(green - average)
        return diff >= threshold
    }
}