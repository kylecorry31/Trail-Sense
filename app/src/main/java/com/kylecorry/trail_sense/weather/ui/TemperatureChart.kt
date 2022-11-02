package com.kylecorry.trail_sense.weather.ui

import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.chart.Chart
import com.kylecorry.trail_sense.shared.views.chart.LineChartData


class TemperatureChart(private val chart: Chart) {

    private val color = Resources.getAndroidColorAttr(chart.context, R.attr.colorPrimary)

    init {
        chart.configureXAxis(labelCount = 0, drawGridLines = false)
        chart.configureYAxis(labelCount = 5, drawGridLines = true)
    }

    fun plot(data: List<Reading<Float>>) {
        val values = Chart.getDataFromReadings(data) { it }
        chart.plot(LineChartData(values, color))
    }
}