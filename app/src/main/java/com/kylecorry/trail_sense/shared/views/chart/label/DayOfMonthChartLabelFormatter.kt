package com.kylecorry.trail_sense.shared.views.chart.label

import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.views.chart.label.ChartLabelFormatter

// X-axis labels for monthly pedometer chart (1, 5, 10, ...)
class DayOfMonthChartLabelFormatter : ChartLabelFormatter {
    override fun format(value: Float): String {
        return tryOrDefault("") {
            val day = Math.round(value)
            if (day in 1..31) day.toString() else ""
        }
    }
}
