package com.kylecorry.trail_sense.tools.metaldetector.ui

import android.graphics.Color
import com.kylecorry.sol.math.Vector2
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.colors.ColorUtils.withAlpha
import com.kylecorry.ceres.chart.Chart
import com.kylecorry.ceres.chart.data.AreaChartLayer
import com.kylecorry.ceres.chart.data.LineChartLayer
import kotlin.math.max
import kotlin.math.min

class MetalDetectorChart(private val chart: Chart, color: Int) {

    private val thresholdArea = AreaChartLayer(
        emptyList(),
        initialLineColor = Color.TRANSPARENT,
        initialAreaColor = AppColor.Gray.color.withAlpha(50)
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
    }

    fun plot(data: List<Float>, threshold: Float) {
        chart.configureYAxis(
            labelCount = 5,
            drawGridLines = true,
            minimum = min(30f, data.minOrNull() ?: 30f),
            maximum = max(100f, data.maxOrNull() ?: 100f)
        )

        thresholdArea.data =
            listOf(Vector2(0f, threshold), Vector2(data.lastIndex.toFloat(), threshold))
        line.data = Chart.indexedData(data)
    }
}