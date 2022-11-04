package com.kylecorry.trail_sense.tools.metaldetector.ui

import android.graphics.Color
import com.kylecorry.sol.math.Vector2
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.colors.ColorUtils.withAlpha
import com.kylecorry.trail_sense.shared.views.chart.Chart
import com.kylecorry.trail_sense.shared.views.chart.data.AreaChartLayer
import com.kylecorry.trail_sense.shared.views.chart.data.LineChartLayer
import kotlin.math.max
import kotlin.math.min

class MetalDetectorChart(private val chart: Chart, private val color: Int) {

    init {
        chart.configureYAxis(
            labelCount = 5,
            drawGridLines = true
        )

        chart.configureXAxis(
            labelCount = 0,
            drawGridLines = false
        )
    }

    fun plot(data: List<Float>, threshold: Float) {
        chart.configureYAxis(
            labelCount = 5,
            drawGridLines = true,
            minimum = min(30f, data.minOrNull() ?: 30f),
            maximum = max(100f, data.maxOrNull() ?: 100f)
        )

        chart.plot(
            AreaChartLayer(
                listOf(Vector2(0f, threshold), Vector2(data.size.toFloat(), threshold)),
                lineColor = Color.TRANSPARENT,
                areaColor = AppColor.Gray.color.withAlpha(50)
            ),
            LineChartLayer(
                Chart.indexedData(data), color
            )
        )
    }
}