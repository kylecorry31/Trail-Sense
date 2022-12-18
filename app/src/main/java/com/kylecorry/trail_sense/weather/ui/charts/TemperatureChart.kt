package com.kylecorry.trail_sense.weather.ui.charts

import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.ceres.chart.Chart
import com.kylecorry.ceres.chart.data.FullAreaChartLayer
import com.kylecorry.ceres.chart.data.LineChartLayer
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.views.chart.label.HourChartLabelFormatter
import java.time.Instant


class TemperatureChart(private val chart: Chart, showFreezing: Boolean = true) {

    private val color = Resources.getAndroidColorAttr(chart.context, R.attr.colorPrimary)
    private var startTime = Instant.now()

    private val rawLine = LineChartLayer(
        emptyList(),
        AppColor.Gray.color.withAlpha(50)
    )

    private val line = LineChartLayer(
        emptyList(),
        color
    )

    private val freezingArea = FullAreaChartLayer(
        0f,
        -100f,
        AppColor.Gray.color.withAlpha(50)
    )

    init {
        chart.configureXAxis(
            labelCount = 5,
            drawGridLines = true,
            labelFormatter = HourChartLabelFormatter(chart.context) { startTime }
        )
        chart.configureYAxis(labelCount = 5, drawGridLines = true)
        chart.emptyText = chart.context.getString(R.string.no_data)
        if (showFreezing) {
            val freezing = Temperature.celsius(0f).convertTo(UserPreferences(chart.context).temperatureUnits).temperature
            freezingArea.top = freezing
            chart.plot(freezingArea, rawLine, line)
        } else {
            chart.plot(rawLine, line)
        }
    }

    fun plot(data: List<Reading<Float>>, raw: List<Reading<Float>>? = null) {
        startTime = data.firstOrNull()?.time ?: Instant.now()
        val values = Chart.getDataFromReadings(data, startTime) { it }
        val range = Chart.getYRange(values, 5f, 10f)
        chart.configureYAxis(
            labelCount = 5,
            drawGridLines = true,
            minimum = range.start,
            maximum = range.end
        )

        if (raw != null) {
            rawLine.data = Chart.getDataFromReadings(raw, startTime) {
                it
            }
        } else {
            rawLine.data = emptyList()
        }

        line.data = values
    }
}