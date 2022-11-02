package com.kylecorry.trail_sense.shared.views.chart

import com.kylecorry.andromeda.core.math.DecimalFormatter

class NumberChartLabelFormatter(private val places: Int = 0, private val strict: Boolean = true) :
    ChartLabelFormatter {
    override fun format(value: Float): String {
        return DecimalFormatter.format(value, places, strict)
    }
}