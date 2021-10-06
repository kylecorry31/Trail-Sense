package com.kylecorry.trail_sense.weather.domain.clouds

import android.graphics.Color
import com.kylecorry.andromeda.core.specifications.Specification

class IsSunSpecification : Specification<Int>() {
    override fun isSatisfiedBy(value: Int): Boolean {
        val blue = Color.blue(value)
        val red = Color.red(value)
        val green = Color.green(value)
        return red == 255 && blue == 255 && green == 255
    }
}