package com.kylecorry.trail_sense.tools.ballistics.domain

import com.kylecorry.sol.math.interpolation.LinearInterpolator

class TableInterpolator(table: Map<Float, Float>) {

    private val interpolator = LinearInterpolator()
    private val entries = table.entries.sortedBy { it.key }
    private val keys = entries.map { it.key }
    private val values = entries.map { it.value }

    fun interpolate(value: Float): Float {
        return interpolator.interpolate(
            value,
            keys,
            values
        )
    }

}