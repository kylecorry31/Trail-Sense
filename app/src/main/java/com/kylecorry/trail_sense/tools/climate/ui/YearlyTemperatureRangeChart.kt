package com.kylecorry.trail_sense.tools.climate.ui

import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.andromeda.views.chart.Chart
import com.kylecorry.andromeda.views.chart.data.FullAreaChartLayer
import com.kylecorry.andromeda.views.chart.data.LineChartLayer
import com.kylecorry.andromeda.views.chart.data.ScatterChartLayer
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

    private val highlight = ScatterChartLayer(
        emptyList(),
        Resources.androidTextColorPrimary(chart.context),
        8f
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
            labelFormatter = DayOfYearChartLabelFormatter(chart.context, year)
        )
        chart.configureYAxis(labelCount = 5, drawGridLines = true)
        chart.plot(freezingArea, lowLine, highLine, highlight)
        chart.setShouldRerenderEveryCycle(false)
    }

    fun highlight(date: LocalDate) {
        val x = date.dayOfYear
        val low = lowLine.data.firstOrNull { it.x.toInt() == x }
        val high = highLine.data.firstOrNull { it.x.toInt() == x }
        highlight.data = listOfNotNull(low, high)
        chart.invalidate()
    }

    fun plot(data: List<Pair<LocalDate, Range<Temperature>>>, units: TemperatureUnits) {
        val freezing = Temperature.celsius(0f).convertTo(units)
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
            labelFormatter = DayOfYearChartLabelFormatter(chart.context, year)
        )
        freezingArea.top = freezing.temperature
        freezingArea.bottom = range.start
        lowLine.data = lows
        highLine.data = highs
        chart.invalidate()
    }
}