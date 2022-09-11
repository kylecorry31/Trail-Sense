package com.kylecorry.trail_sense.tools.metaldetector.ui

import com.github.mikephil.charting.charts.LineChart
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.SimpleLineChart
import kotlin.math.max
import kotlin.math.min


class MetalDetectorChart(chart: LineChart, private val color: Int) {

    private val simpleChart = SimpleLineChart(chart, chart.context.getString(R.string.no_data))

    private var granularity = 1f

    init {
        simpleChart.configureYAxis(
            granularity = granularity,
            labelCount = 5,
            drawGridLines = true
        )

        simpleChart.configureXAxis(
            labelCount = 0,
            drawGridLines = false
        )
    }

    fun plot(data: List<Float>) {
        simpleChart.configureYAxis(
            granularity = granularity,
            labelCount = 5,
            drawGridLines = true,
            minimum = min(30f, data.minOrNull() ?: 30f),
            maximum = max(100f, data.maxOrNull() ?: 100f)
        )

        simpleChart.plotIndexed(data, color, false)
    }
}