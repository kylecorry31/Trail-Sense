package com.kylecorry.trail_sense.weather.domain.clouds

import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.sol.math.SolMath.map
import com.kylecorry.trail_sense.shared.colors.ColorUtils

class NRBRIsSkySpecification(private val threshold: Float) : Specification<Int>() {
    override fun isSatisfiedBy(value: Int): Boolean {
        return map(ColorUtils.nrbr(value), -1f, 1f, 0f, 1f) <= threshold
    }
}