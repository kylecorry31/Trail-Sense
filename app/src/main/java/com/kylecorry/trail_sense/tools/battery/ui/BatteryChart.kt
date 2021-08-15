package com.kylecorry.trail_sense.tools.battery.ui

import com.github.mikephil.charting.charts.LineChart
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.SimpleLineChart
import com.kylecorry.trailsensecore.domain.power.BatteryReading

class BatteryChart(chart: LineChart) {

    private val simpleChart = SimpleLineChart(chart, chart.context.getString(R.string.no_data))
    private val context = chart.context

    init {
        simpleChart.configureYAxis(
            minimum = 0f,
            maximum = 100f,
            granularity = 20f,
            labelCount = 5,
            drawGridLines = true
        )

        simpleChart.configureXAxis(
            labelCount = 0,
            drawGridLines = false
        )
    }

    fun plot(readings: List<BatteryReading>, showCapacity: Boolean = false){
        val data = readings.map {
            it.time.toEpochMilli().toFloat() to if (showCapacity) it.capacity else it.percent
        }

        simpleChart.plot(data, Resources.color(context, R.color.colorPrimary), true)
    }

}