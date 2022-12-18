package com.kylecorry.trail_sense.tools.battery.ui

import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.ceres.chart.Chart
import com.kylecorry.ceres.chart.data.AreaChartLayer
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.tools.battery.domain.BatteryReading

class BatteryChart(private val chart: Chart) {

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

        chart.emptyText = chart.context.getString(R.string.no_data)
    }

    fun plot(readings: List<BatteryReading>, showCapacity: Boolean = false) {
        val data = Chart.getDataFromReadings(readings.map {
            Reading(
                if (showCapacity) it.capacity else it.percent,
                it.time
            )
        }) {
            it
        }

        val color = AppColor.Orange.color

        chart.plot(
            AreaChartLayer(data, color, color.withAlpha(150)),
        )
    }

}