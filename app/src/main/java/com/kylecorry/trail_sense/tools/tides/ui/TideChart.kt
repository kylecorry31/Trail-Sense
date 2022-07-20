package com.kylecorry.trail_sense.tools.tides.ui

import com.github.mikephil.charting.charts.LineChart
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.SolMath.norm
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.views.SimpleLineChart
import java.time.Instant


class TideChart(private val chart: LineChart) {

    private val simpleChart = SimpleLineChart(chart, chart.context.getString(R.string.no_data))
    private val color = AppColor.Blue.color
    private var startTime = Instant.now()

    init {
        simpleChart.configureYAxis(
            labelCount = 0,
            drawGridLines = false,
            minimum = 0f,
            maximum = 1f,
        )

        simpleChart.configureXAxis(
            labelCount = 7,
            drawGridLines = false,
            labelFormatter = SimpleLineChart.hourLabelFormatter(chart.context) { startTime }
        )
    }

    fun plot(data: List<Reading<Float>>, range: Range<Float>) {
        val first = data.firstOrNull()?.time
        startTime = first
        val values = SimpleLineChart.getDataFromReadings(data, startTime) {
            norm(it, range.start - 0.5f, range.end + 0.5f)
        }
        simpleChart.plot(values, color, filled = true)
    }

    fun getPoint(index: Int): PixelCoordinate {
        val point = tryOrDefault(SimpleLineChart.Point(0, index, 0f, 0f)) {
            simpleChart.getPoint(0, index)
        }

        return PixelCoordinate(point.x + chart.x, point.y + chart.y)
    }
}