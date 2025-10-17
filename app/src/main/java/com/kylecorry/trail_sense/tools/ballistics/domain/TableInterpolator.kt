package com.kylecorry.trail_sense.tools.ballistics.domain

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.interpolation.LinearInterpolator

class TableInterpolator(table: Map<Float, Float>) {

    private val points = table.entries.map { Vector2(it.key, it.value) }
    private val interpolator = LinearInterpolator(points)

    fun interpolate(value: Float): Float {
        return interpolator.interpolate(value)
    }

}