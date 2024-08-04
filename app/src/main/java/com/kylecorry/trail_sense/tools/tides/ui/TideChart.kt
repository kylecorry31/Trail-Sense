package com.kylecorry.trail_sense.tools.tides.ui

import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.andromeda.views.chart.Chart
import com.kylecorry.andromeda.views.chart.data.AreaChartLayer
import com.kylecorry.andromeda.views.chart.data.ScatterChartLayer
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.SolMath.norm
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryColor
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.views.chart.label.HourChartLabelFormatter
import java.time.Instant


class TideChart(private val chart: Chart) {

    private var startTime = Instant.now()

    private val levelColor = if (UserPreferences(chart.context).useDynamicColors) {
        Resources.getPrimaryColor(chart.context)
    } else {
        AppColor.Blue.color
    }

    private val highlight = ScatterChartLayer(
        emptyList(),
        Resources.androidTextColorPrimary(chart.context),
        8f
    )

    private val level = AreaChartLayer(
        emptyList(),
        levelColor,
        levelColor.withAlpha(50)
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
            drawGridLines = true,
            labelFormatter = HourChartLabelFormatter(chart.context) { startTime }
        )

        chart.emptyText = chart.context.getString(R.string.no_data)

        chart.plot(level, highlight)

        chart.setShouldRerenderEveryCycle(false)
    }

    fun plot(data: List<Reading<Float>>, range: Range<Float>) {
        val first = data.firstOrNull()?.time
        startTime = first
        level.data = convert(data, range)
        chart.invalidate()
    }

    fun highlight(point: Reading<Float>, range: Range<Float>) {
        val value = convert(listOf(point), range)
        highlight.data = value
        chart.invalidate()
    }

    fun removeHighlight() {
        highlight.data = emptyList()
        chart.invalidate()
    }

    private fun convert(readings: List<Reading<Float>>, range: Range<Float>): List<Vector2> {
        val totalRange = range.end - range.start
        val rangeDelta = (totalRange * 0.25f).coerceAtLeast(0.2f)
        return Chart.getDataFromReadings(readings, startTime) {
            norm(it, range.start - rangeDelta, range.end + rangeDelta)
        }
    }
}