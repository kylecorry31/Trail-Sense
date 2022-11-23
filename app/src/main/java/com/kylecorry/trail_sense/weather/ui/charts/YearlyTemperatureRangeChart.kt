package com.kylecorry.trail_sense.weather.ui.charts

import com.kylecorry.ceres.chart.Chart
import com.kylecorry.ceres.chart.data.LineChartLayer
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.trail_sense.shared.colors.AppColor
import java.time.LocalDate


class YearlyTemperatureRangeChart(
    private val chart: Chart,
    private val onClick: (dayOfYear: Int) -> Unit
) {

    private val lowLine = LineChartLayer(emptyList(), AppColor.Blue.color) {
        onClick(it.x.toInt())
        true
    }

    private val highLine = LineChartLayer(emptyList(), AppColor.Red.color) {
        onClick(it.x.toInt())
        true
    }

    init {
        chart.configureXAxis(
            labelCount = 5,
            drawGridLines = true,
            labelFormatter = MonthChartLabelFormatter(chart.context, LocalDate.now().year)
        )
        chart.configureYAxis(labelCount = 5, drawGridLines = true)
        chart.plot(lowLine, highLine)
    }

    fun plot(data: List<Pair<Int, Range<Temperature>>>, units: TemperatureUnits, year: Int) {
        val lows = data.map {
            Vector2(
                it.first.toFloat(),
                it.second.start.convertTo(units).temperature
            )
        }
        val highs = data.map {
            Vector2(
                it.first.toFloat(),
                it.second.start.convertTo(units).temperature
            )
        }
        val range = Chart.getYRange(lows + highs, 5f, 10f)
        chart.configureXAxis(
            labelCount = 5,
            drawGridLines = true,
            labelFormatter = MonthChartLabelFormatter(chart.context, year)
        )
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