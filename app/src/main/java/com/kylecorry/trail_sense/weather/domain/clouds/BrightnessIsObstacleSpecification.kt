package com.kylecorry.trail_sense.weather.domain.clouds

import android.graphics.Color
import com.kylecorry.andromeda.core.specifications.Specification

class BrightnessIsObstacleSpecification(private val threshold: Float) : Specification<Int>() {
    override fun isSatisfiedBy(value: Int): Boolean {
        val blue = Color.blue(value)
        val red = Color.red(value)
        val green = Color.green(value)

        val avg = (red + blue + green) / 3f

        return avg < threshold
    }
}