package com.kylecorry.trail_sense.tools.metaldetector.ui

import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.andromeda.views.chart.Chart
import com.kylecorry.andromeda.views.chart.data.BoundsChartLayer
import com.kylecorry.andromeda.views.chart.data.LineChartLayer
import com.kylecorry.sol.math.Vector2
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.colors.AppColor
import kotlin.math.max
import kotlin.math.min

class MetalDetectorChart(private val chart: Chart, color: Int) {

    private val thresholdArea = BoundsChartLayer(
        emptyList(),
        emptyList(),
        initialColor = AppColor.Gray.color.withAlpha(50)
    )

    private val line = LineChartLayer(
        emptyList(),
        color
    )

    init {
        chart.configureYAxis(
            labelCount = 5,
            drawGridLines = true
        )

        chart.configureXAxis(
            labelCount = 0,
            drawGridLines = false
        )

        chart.emptyText = chart.context.getString(R.string.no_data)

        chart.plot(thresholdArea, line)

        chart.setShouldRerenderEveryCycle(false)
    }

    fun plot(data: List<Float>, lowerThreshold: Float, upperThreshold: Float) {
        val minimum = 30f
        val maximum = 70f

        chart.configureYAxis(
            labelCount = 5,
            drawGridLines = true,
            minimum = min(minimum, data.minOrNull() ?: minimum),
            maximum = max(maximum, data.maxOrNull() ?: maximum)
        )

        thresholdArea.lower = listOf(Vector2(0f, lowerThreshold), Vector2(data.lastIndex.toFloat(), lowerThreshold))
        thresholdArea.upper = listOf(Vector2(0f, upperThreshold), Vector2(data.lastIndex.toFloat(), upperThreshold))

        line.data = Chart.indexedData(data)

        chart.invalidate()
    }
}