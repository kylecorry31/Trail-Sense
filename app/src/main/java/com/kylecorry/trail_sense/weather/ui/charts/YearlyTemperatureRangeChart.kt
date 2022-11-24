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
    private val onClick: (date: LocalDate) -> Unit
) {

    private var year = 2000

    private val lowLine = LineChartLayer(emptyList(), AppColor.Blue.color) {
        onClick(LocalDate.ofYearDay(year, it.x.toInt()))
        true
    }

    private val highLine = LineChartLayer(emptyList(), AppColor.Red.color) {
        onClick(LocalDate.ofYearDay(year, it.x.toInt()))
        true
    }

    init {
        chart.configureXAxis(
            labelCount = 5,
            drawGridLines = true,
            labelFormatter = MonthChartLabelFormatter(chart.context, year)
        )
        chart.configureYAxis(labelCount = 5, drawGridLines = true)
        chart.plot(lowLine, highLine)
    }

    fun plot(data: List<Pair<LocalDate, Range<Temperature>>>, units: TemperatureUnits) {
        val lows = data.map {
            Vector2(
                it.first.dayOfYear.toFloat(),
                it.second.start.convertTo(units).temperature
            )
        }
        val highs = data.map {
            Vector2(
                it.first.dayOfYear.toFloat(),
                it.second.end.convertTo(units).temperature
            )
        }
        year = data.firstOrNull()?.first?.year ?: 2000
        val range = Chart.getYRange(lows + highs, 5f, 10f)
        chart.configureYAxis(
            labelCount = 5,
            drawGridLines = true,
            minimum = range.start,
            maximum = range.end
        )
        chart.configureXAxis(
            labelCount = 5,
            drawGridLines = true,
            labelFormatter = MonthChartLabelFormatter(chart.context, year)
        )
        lowLine.data = lows
        highLine.data = highs
    }
}