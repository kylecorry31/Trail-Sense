package com.kylecorry.trail_sense.weather.ui.charts

import com.kylecorry.ceres.chart.Chart
import com.kylecorry.ceres.chart.data.LineChartLayer
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.trail_sense.shared.colors.AppColor
import java.time.Month


class YearlyTemperatureRangeChart(
    private val chart: Chart,
    private val onClick: (month: Month) -> Unit
) {

    private val lowLine = LineChartLayer(emptyList(), AppColor.Blue.color) {
        onClick(Month.of(it.x.toInt()))
        true
    }

    private val highLine = LineChartLayer(emptyList(), AppColor.Red.color) {
        onClick(Month.of(it.x.toInt()))
        true
    }

    init {
        chart.configureXAxis(
            labelCount = 5,
            drawGridLines = true,
            labelFormatter = MonthChartLabelFormatter(chart.context)
        )
        chart.configureYAxis(labelCount = 5, drawGridLines = true)
        chart.plot(lowLine, highLine)
    }

    fun plot(data: List<Pair<Month, Range<Temperature>>>, units: TemperatureUnits) {
        val lows = data.map {
            Vector2(
                it.first.value.toFloat(),
                it.second.start.convertTo(units).temperature
            )
        }
        val highs = data.map {
            Vector2(
                it.first.value.toFloat(),
                it.second.end.convertTo(units).temperature
            )
        }
        val range = Chart.getYRange(lows + highs, 5f, 10f)
        chart.configureYAxis(
            labelCount = 5,
            drawGridLines = true,
            minimum = range.start,
            maximum = range.end
        )
        lowLine.data = lows
        highLine.data = highs
    }
}