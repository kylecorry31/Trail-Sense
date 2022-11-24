package com.kylecorry.trail_sense.weather.ui.charts

import android.content.Context
import com.kylecorry.ceres.chart.label.ChartLabelFormatter
import com.kylecorry.trail_sense.shared.FormatService
import java.time.LocalDate

class MonthChartLabelFormatter(context: Context, private val year: Int) : ChartLabelFormatter {

    private val formatter = FormatService.getInstance(context)

    override fun format(value: Float): String {
        val dayOfYear = value.toInt()
        val date = LocalDate.ofYearDay(year, dayOfYear)
        return formatter.formatMonth(date.month, true)
    }
}