package com.kylecorry.trail_sense.shared.views.chart.label

import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.views.chart.label.ChartLabelFormatter
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// X-axis labels for weekly pedometer chart (Mon, Tue, ...)
class DayOfWeekChartLabelFormatter(
    private val weekStart: LocalDate
) : ChartLabelFormatter {
    override fun format(value: Float): String {
        return tryOrDefault("") {
            val dayIndex = value.toInt() - 1
            if (dayIndex in 0..6) {
                weekStart.plusDays(dayIndex.toLong()).dayOfWeek
                    .getDisplayName(TextStyle.SHORT, Locale.getDefault())
            } else ""
        }
    }
}
