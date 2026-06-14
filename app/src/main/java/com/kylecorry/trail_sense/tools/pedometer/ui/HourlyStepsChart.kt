package com.kylecorry.trail_sense.tools.pedometer.ui

import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.views.chart.Chart
import com.kylecorry.andromeda.views.chart.data.HistogramChartLayer
import com.kylecorry.andromeda.views.chart.data.ScatterChartLayer
import com.kylecorry.sol.math.Vector2
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryColor
import com.kylecorry.trail_sense.shared.views.chart.label.HourChartLabelFormatter
import com.kylecorry.trail_sense.tools.pedometer.domain.HourlyStepCount

class HourlyStepsChart(
    private val chart: Chart,
    private val onClick: (hourlyStepCount: HourlyStepCount) -> Unit
) {

    private var steps = emptyList<HourlyStepCount>()

    private val bars = HistogramChartLayer(emptyList(), Resources.getPrimaryColor(chart.context), width = 0.8f) {
        val hour = it.x.toInt()
        steps.getOrNull(hour)?.let(onClick)
        true
    }

    private val highlight = ScatterChartLayer(
        emptyList(),
        Resources.androidTextColorPrimary(chart.context),
        8f
    )

    init {
        chart.configureXAxis(
            labelCount = 7,
            drawGridLines = true,
            labelFormatter = HourChartLabelFormatter(chart.context) {
                steps.firstOrNull()?.startTime ?: java.time.Instant.now()
            },
            minimum = 0f,
            maximum = 23f
        )
        chart.configureYAxis(labelCount = 5, drawGridLines = true, minimum = 0f)
        chart.plot(bars, highlight)
        chart.setShouldRerenderEveryCycle(false)
    }

    fun highlight(hourlyStepCount: HourlyStepCount?) {
        val index = hourlyStepCount?.let { steps.indexOf(it) } ?: -1
        val selected = hourlyStepCount
        highlight.data = if (index >= 0 && selected != null) {
            listOf(Vector2(index.toFloat(), selected.steps.toFloat()))
        } else {
            emptyList()
        }
        chart.invalidate()
    }

    fun plot(data: List<HourlyStepCount>) {
        steps = data
        val values = data.mapIndexed { index, hourlyStepCount ->
            Vector2(index.toFloat(), hourlyStepCount.steps.toFloat())
        }
        val range = Chart.getYRange(values, 100f, 500f)
        chart.configureYAxis(
            labelCount = 5,
            drawGridLines = true,
            minimum = 0f,
            maximum = range.end
        )
        chart.configureXAxis(
            labelCount = 7,
            drawGridLines = true,
            labelFormatter = HourChartLabelFormatter(chart.context) {
                steps.firstOrNull()?.startTime ?: java.time.Instant.now()
            },
            minimum = 0f,
            maximum = (data.size - 1).coerceAtLeast(0).toFloat()
        )
        bars.data = values
        chart.invalidate()
    }
}
