package com.kylecorry.trail_sense.tools.pedometer.ui

import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.views.chart.Chart
import com.kylecorry.andromeda.views.chart.data.HistogramChartLayer
import com.kylecorry.andromeda.views.chart.data.ScatterChartLayer
import com.kylecorry.sol.math.Vector2
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryColor
import com.kylecorry.trail_sense.shared.views.chart.label.HourChartLabelFormatter
import com.kylecorry.trail_sense.tools.pedometer.domain.HourlyStepCount
import java.time.Instant

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
        configureAxes(maxHour = 23f, maxSteps = null)
        chart.plot(bars, highlight)
        chart.setShouldRerenderEveryCycle(false)
    }

    fun highlight(hourlyStepCount: HourlyStepCount?) {
        val index = hourlyStepCount?.let { steps.indexOf(it) } ?: -1
        highlight.data = if (hourlyStepCount != null && index >= 0) {
            listOf(Vector2(index.toFloat(), hourlyStepCount.steps.toFloat()))
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
        configureAxes(maxHour = (data.size - 1).coerceAtLeast(0).toFloat(), maxSteps = range.end)
        bars.data = values
        chart.invalidate()
    }

    private fun configureAxes(maxHour: Float, maxSteps: Float?) {
        chart.configureXAxis(
            labelCount = 7,
            drawGridLines = true,
            labelFormatter = HourChartLabelFormatter(chart.context) {
                steps.firstOrNull()?.startTime ?: Instant.now()
            },
            minimum = 0f,
            maximum = maxHour
        )
        chart.configureYAxis(
            labelCount = 5,
            drawGridLines = true,
            minimum = 0f,
            maximum = maxSteps
        )
    }
}
