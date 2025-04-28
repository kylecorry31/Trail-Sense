package com.kylecorry.trail_sense.tools.climate.ui

import android.content.Context
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.views.chart.label.ChartLabelFormatter
import com.kylecorry.trail_sense.shared.FormatService
import java.time.Month

class MonthChartLabelFormatter(context: Context) : ChartLabelFormatter {

    private val formatter = FormatService.getInstance(context)

    override fun format(value: Float): String {
        return tryOrDefault("") {
            formatter.formatMonth(Month.of(value.toInt()), true)
        }
    }
}