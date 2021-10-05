package com.kylecorry.trail_sense.weather.domain.clouds

import android.graphics.Color
import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.sol.math.SolMath.map

class NRBRIsSkySpecification(private val threshold: Float) : Specification<Int>() {
    override fun isSatisfiedBy(value: Int): Boolean {
        val blue = Color.blue(value)
        val red = Color.red(value)

        val nrbr = (red - blue) / (red + blue).toFloat().coerceAtLeast(1f)

        return map(nrbr, -1f, 1f, 0f, 1f) <= threshold
    }
}