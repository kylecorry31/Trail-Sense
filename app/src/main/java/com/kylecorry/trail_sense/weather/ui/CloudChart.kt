package com.kylecorry.trail_sense.weather.ui

import com.github.mikephil.charting.charts.LineChart
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.SimpleLineChart
import com.kylecorry.trail_sense.weather.domain.CloudObservation


class CloudChart(chart: LineChart) {

    private val simpleChart = SimpleLineChart(chart, chart.context.getString(R.string.no_data))

    private var granularity = 1f

    private val color = Resources.color(chart.context, R.color.colorPrimary)

    init {
        simpleChart.configureYAxis(
            granularity = granularity,
            labelCount = 5,
            drawGridLines = true,
            minimum = 0f,
            maximum = 100f
        )

        simpleChart.configureXAxis(
            labelCount = 0,
            drawGridLines = false
        )
    }

    fun plot(data: List<Reading<CloudObservation>>) {
        val values =
            data.map { ((it.time.toEpochMilli() / 1000) % 1000000).toFloat() to (it.value.coverage * 100) }
        simpleChart.plot(values, color)
    }
}