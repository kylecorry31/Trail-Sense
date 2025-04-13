package com.kylecorry.trail_sense.tools.ballistics.domain

import com.kylecorry.sol.math.interpolation.NewtonInterpolator

class TableInterpolator(table: Map<Float, Float>) {

    private val newtonInterpolator = NewtonInterpolator()
    private val entries = table.entries.sortedBy { it.key }
    private val keys = entries.map { it.key }
    private val values = entries.map { it.value }

    fun interpolate(value: Float): Float {
        return newtonInterpolator.interpolate(
            value,
            keys,
            values
        )
    }

}