package com.kylecorry.trail_sense.weather.domain.clouds.mask

import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.trail_sense.shared.colors.ColorUtils

class BrightnessIsObstacleSpecification(private val threshold: Float) : Specification<Int>() {
    override fun isSatisfiedBy(value: Int): Boolean {
        return ColorUtils.average(value) < threshold
    }
}