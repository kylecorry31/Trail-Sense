package com.kylecorry.trail_sense.weather.domain.clouds

import android.graphics.Color
import com.kylecorry.andromeda.core.specifications.Specification

class BGIsSkySpecification(private val threshold: Int) : Specification<Int>() {
    override fun isSatisfiedBy(value: Int): Boolean {
        val blue = Color.blue(value)
        val green = Color.green(value)
        val bg = blue - green
        return bg >= threshold
    }
}