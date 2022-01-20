package com.kylecorry.trail_sense.weather.domain.clouds.mask

import android.graphics.Color
import com.kylecorry.andromeda.core.specifications.Specification

class IsSunSpecification : Specification<Int>() {
    override fun isSatisfiedBy(value: Int): Boolean {
        return value == Color.WHITE
    }
}