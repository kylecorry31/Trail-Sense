package com.kylecorry.trail_sense.tools.tides.ui

import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.SolMath.norm
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.colors.ColorUtils.withAlpha
import com.kylecorry.ceres.chart.Chart
import com.kylecorry.ceres.chart.data.AreaChartLayer
import com.kylecorry.ceres.chart.data.ScatterChartLayer
import com.kylecorry.trail_sense.shared.views.chart.label.HourChartLabelFormatter
import java.time.Instant


class TideChart(chart: Chart) {

    private var startTime = Instant.now()

    private val highlight = ScatterChartLayer(
        emptyList(),
        Resources.androidTextColorPrimary(chart.context),
        8f
    )

    private val level = AreaChartLayer(
        emptyList(),
        AppColor.Blue.color,
        AppColor.Blue.color.withAlpha(50)
    )

    init {
        chart.configureYAxis(
            labelCount = 0,
            drawGridLines = false,
            minimum = 0f,
            maximum = 1f,
        )

        chart.configureXAxis(
            labelCount = 7,
            drawGridLines = false,
            labelFormatter = HourChartLabelFormatter(chart.context) { startTime }
        )

        chart.emptyText = chart.context.getString(R.string.no_data)

        chart.plot(level, highlight)
    }

    fun plot(data: List<Reading<Float>>, range: Range<Float>) {
        val first = data.firstOrNull()?.time
        startTime = first
        level.data = convert(data, range)
    }

    fun highlight(point: Reading<Float>, range: Range<Float>) {
        val value = convert(listOf(point), range)
        highlight.data = value
    }

    fun removeHighlight() {
        highlight.data = emptyList()
    }

    private fun convert(readings: List<Reading<Float>>, range: Range<Float>): List<Vector2> {
        return Chart.getDataFromReadings(readings, startTime) {
            norm(it, range.start - 0.5f, range.end + 0.5f)
        }
    }
}