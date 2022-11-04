package com.kylecorry.trail_sense.weather.ui

import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.chart.Chart
import com.kylecorry.trail_sense.shared.views.chart.data.LineChartLayer

class HumidityChart(private val chart: Chart) {

    private val color = Resources.getAndroidColorAttr(chart.context, R.attr.colorPrimary)

    init {
        chart.configureYAxis(
            minimum = 0f,
            maximum = 100f,
            labelCount = 5,
            drawGridLines = true
        )

        chart.configureXAxis(
            labelCount = 0,
            drawGridLines = false
        )
    }

    fun plot(data: List<Reading<Float>>) {
        val values = Chart.getDataFromReadings(data) { it }
        chart.plot(LineChartLayer(values, color))
    }
}