package com.kylecorry.trail_sense.weather.ui.charts

import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.ceres.chart.Chart
import com.kylecorry.ceres.chart.data.LineChartLayer
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.chart.label.HourChartLabelFormatter
import java.time.Instant


class TemperatureChart(private val chart: Chart) {

    private val color = Resources.getAndroidColorAttr(chart.context, R.attr.colorPrimary)
    private var startTime = Instant.now()

    init {
        chart.configureXAxis(
            labelCount = 5,
            drawGridLines = true,
            labelFormatter = HourChartLabelFormatter(chart.context) { startTime }
        )
        chart.configureYAxis(labelCount = 5, drawGridLines = true)
    }

    fun plot(data: List<Reading<Float>>) {
        startTime = data.firstOrNull()?.time ?: Instant.now()
        val values = Chart.getDataFromReadings(data, startTime) { it }
        val range = Chart.getYRange(values, 5f, 10f)
        chart.configureYAxis(
            labelCount = 5,
            drawGridLines = true,
            minimum = range.start,
            maximum = range.end
        )
        chart.plot(LineChartLayer(values, color))
    }
}