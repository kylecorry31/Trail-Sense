package com.kylecorry.trail_sense.tools.pedometer.ui

import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.views.chart.Chart
import com.kylecorry.andromeda.views.chart.data.LineChartLayer
import com.kylecorry.andromeda.views.chart.data.ScatterChartLayer
import com.kylecorry.andromeda.views.chart.label.ChartLabelFormatter
import com.kylecorry.sol.math.Vector2
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryColor
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow

class PedometerChart(private val chart: Chart) {

    private val color = Resources.getPrimaryColor(chart.context)

    private val line = LineChartLayer(emptyList(), color, 2.5f)
    private val dots = ScatterChartLayer(emptyList(), color, 6f)

    init {
        chart.configureYAxis(
            minimum = 0f,
            labelCount = 5,
            drawGridLines = true
        )
        chart.configureXAxis(
            labelCount = 0,
            drawGridLines = false
        )
        chart.emptyText = ""
        chart.plot(line, dots)
        chart.setShouldRerenderEveryCycle(false)
    }

    fun plot(
        data: List<Pair<Float, Float>>,
        xMin: Float = 0f,
        xMax: Float,
        xLabelCount: Int = 0,
        xLabelFormatter: ChartLabelFormatter? = null
    ) {
        val points = data.map { Vector2(it.first, it.second) }
        val maxY = points.maxOfOrNull { it.y } ?: 0f
        val niceMax = niceYMax(maxY)

        chart.configureYAxis(
            minimum = 0f,
            maximum = niceMax,
            labelCount = 5,
            drawGridLines = true
        )

        if (xLabelFormatter != null) {
            chart.configureXAxis(
                minimum = xMin,
                maximum = xMax,
                labelCount = xLabelCount,
                drawGridLines = false,
                labelFormatter = xLabelFormatter
            )
        } else {
            chart.configureXAxis(
                minimum = xMin,
                maximum = xMax,
                labelCount = xLabelCount,
                drawGridLines = false
            )
        }

        line.data = points
        dots.data = points
        chart.invalidate()
    }

    companion object {
        /**
         * Round max value up to a "nice" number for the Y axis ceiling.
         * Produces round numbers like 500, 1000, 2000, 5000, 10000, etc.
         */
        fun niceYMax(value: Float): Float {
            if (value <= 0f) return 500f
            val magnitude = 10f.pow((Math.log10(value.toDouble()) - 1).toInt().toFloat())
            val step = when {
                value <= magnitude * 2 -> magnitude * 2
                value <= magnitude * 5 -> magnitude * 5
                else -> magnitude * 10
            }
            return max(step, ceil(value / step) * step)
        }
    }
}
