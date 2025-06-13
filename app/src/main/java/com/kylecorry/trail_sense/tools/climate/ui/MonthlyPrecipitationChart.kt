package com.kylecorry.trail_sense.tools.climate.ui

import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.views.chart.Chart
import com.kylecorry.andromeda.views.chart.data.HistogramChartLayer
import com.kylecorry.andromeda.views.chart.data.ScatterChartLayer
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.shared.colors.AppColor
import java.time.Month


class MonthlyPrecipitationChart(
    private val chart: Chart,
    private val onClick: (month: Month) -> Unit
) {

    private val bars = HistogramChartLayer(emptyList(), AppColor.Blue.color, width = 0.6f) {
        onClick(Month.of(it.x.toInt()))
        true
    }

    private val highlight = ScatterChartLayer(
        emptyList(),
        Resources.androidTextColorPrimary(chart.context),
        8f
    )

    init {
        chart.configureXAxis(
            labelCount = 5,
            drawGridLines = true,
            labelFormatter = MonthChartLabelFormatter(chart.context)
        )
        chart.configureYAxis(labelCount = 14, drawGridLines = true)
        chart.plot(bars, highlight)
        chart.setShouldRerenderEveryCycle(false)
    }

    fun highlight(month: Month) {
        val x = month.value
        val value = bars.data.firstOrNull { it.x.toInt() == x }
        highlight.data = listOfNotNull(value)
        chart.invalidate()
    }

    fun plot(data: Map<Month, Distance>, units: DistanceUnits) {
        val values = data.map {
            Vector2(
                it.key.value.toFloat(),
                it.value.convertTo(units).distance
            )
        }.sortedBy { it.x }
        val range = Chart.getYRange(
            values,
            Distance(0.5f, DistanceUnits.Inches).convertTo(units).distance,
            Distance(1f, DistanceUnits.Inches).convertTo(units).distance
        )
        chart.configureYAxis(
            labelCount = 5,
            drawGridLines = true,
            minimum = 0f,
            maximum = range.end
        )
        chart.configureXAxis(
            labelCount = 14,
            drawGridLines = true,
            labelFormatter = MonthChartLabelFormatter(chart.context),
            minimum = 0f,
            maximum = 13f
        )
        bars.data = values
        chart.invalidate()
    }
}