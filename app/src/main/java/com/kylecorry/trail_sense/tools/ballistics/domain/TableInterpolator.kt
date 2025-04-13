package com.kylecorry.trail_sense.tools.ballistics.domain

class TableInterpolator {

    fun interpolate(value: Float, table: Map<Float, Float>): Float {
        val minTable = table.keys.minOrNull()
        val maxTable = table.keys.maxOrNull()

        if (minTable == null || maxTable == null || value.isNaN()) {
            return 0f
        }

        if (value < minTable) {
            return table[minTable] ?: 0f
        }

        if (value > maxTable) {
            return table[maxTable] ?: 0f
        }

        val before = table.keys.filter { it <= value }.max()
        val after = table.keys.filter { it >= value }.min()

        val beforeValue = table[before] ?: 0f
        val afterValue = table[after] ?: 0f

        return beforeValue + (afterValue - beforeValue) * (value - before) / (after - before)
    }

}