package com.kylecorry.trail_sense.weather.infrastructure.clouds

import android.graphics.Color
import com.kylecorry.andromeda.core.specifications.Specification

class LuminanceIsObstacleSpecification(private val threshold: Int) : Specification<Int>() {
    override fun isSatisfiedBy(value: Int): Boolean {
        val average = (Color.green(value) + Color.blue(value) + Color.red(value)) / 3f
        return average <= threshold
    }
}