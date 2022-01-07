package com.kylecorry.trail_sense.tools.tides.ui

import com.github.mikephil.charting.charts.LineChart
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.views.SimpleLineChart
import java.time.Duration


class TideChart(private val chart: LineChart) {

    private val simpleChart = SimpleLineChart(chart, chart.context.getString(R.string.no_data))

    private val color = AppColor.Blue.color

    init {
        simpleChart.configureYAxis(
            labelCount = 0,
            drawGridLines = false,
            minimum = 0f,
            maximum = 4f
        )

        simpleChart.configureXAxis(
            labelCount = 0,
            drawGridLines = false
        )
    }

    fun plot(data: List<Reading<Float>>) {
        // TODO: fill below the chart and automatically find min / max
        val first = data.firstOrNull()?.time
        val values = data.map { Duration.between(first, it.time).seconds / (60f * 60f) to it.value + 2f }
        simpleChart.plot(values, color, filled = true)
    }

    fun getPoint(index: Int): PixelCoordinate {
        val point = simpleChart.getPoint(0, index)
        return PixelCoordinate(point.x + chart.x, point.y + chart.y)
    }
}