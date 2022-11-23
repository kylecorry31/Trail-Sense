package com.kylecorry.trail_sense.weather.ui.charts

import android.content.Context
import com.kylecorry.ceres.chart.label.ChartLabelFormatter
import com.kylecorry.trail_sense.shared.FormatService
import java.time.Month

class MonthChartLabelFormatter(context: Context) : ChartLabelFormatter {

    private val formatter = FormatService.getInstance(context)

    override fun format(value: Float): String {
        val month = Month.of(value.toInt())
        return formatter.formatMonth(month, true)
    }
}