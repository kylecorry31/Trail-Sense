package com.kylecorry.trail_sense.weather.infrastructure.clouds

import android.graphics.Color
import com.kylecorry.andromeda.core.specifications.Specification

class IsSunSpecification(private val threshold: Int = 255) : Specification<Int>() {
    override fun isSatisfiedBy(value: Int): Boolean {
        return Color.red(value) >= threshold && Color.blue(value) >= threshold && Color.green(value) >= threshold
    }
}