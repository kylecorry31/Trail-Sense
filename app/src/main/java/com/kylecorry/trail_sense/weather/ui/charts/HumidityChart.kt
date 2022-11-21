package com.kylecorry.trail_sense.weather.ui.charts

import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.ceres.chart.Chart
import com.kylecorry.ceres.chart.data.LineChartLayer
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.chart.label.HourChartLabelFormatter
import java.time.Instant

class HumidityChart(private val chart: Chart) {

    private val color = Resources.getAndroidColorAttr(chart.context, R.attr.colorPrimary)
    private var startTime = Instant.now()

    init {
        chart.configureYAxis(
            minimum = 0f,
            maximum = 100f,
            labelCount = 5,
            drawGridLines = true
        )

        chart.configureXAxis(
            labelCount = 0,
            drawGridLines = false,
            labelFormatter = HourChartLabelFormatter(chart.context) { startTime }
        )

        chart.emptyText = chart.context.getString(R.string.no_data)
    }

    fun plot(data: List<Reading<Float>>) {
        startTime = data.firstOrNull()?.time ?: Instant.now()
        val values = Chart.getDataFromReadings(data, startTime) { it }
        chart.plot(LineChartLayer(values, color))
    }
}