package com.kylecorry.trail_sense.tools.climate.ui

import android.content.Context
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.views.chart.label.ChartLabelFormatter
import com.kylecorry.trail_sense.shared.FormatService
import java.time.LocalDate

class DayOfYearChartLabelFormatter(context: Context, private val year: Int) : ChartLabelFormatter {

    private val formatter = FormatService.getInstance(context)

    override fun format(value: Float): String {
        val dayOfYear = value.toInt()
        val date = tryOrDefault(null) {
            LocalDate.ofYearDay(year, dayOfYear)
        } ?: return ""
        return formatter.formatMonth(date.month, true)
    }
}