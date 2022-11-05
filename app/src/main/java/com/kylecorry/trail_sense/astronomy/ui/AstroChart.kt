package com.kylecorry.trail_sense.astronomy.ui

import android.graphics.Color
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.views.chart.Chart
import com.kylecorry.trail_sense.shared.views.chart.data.FullAreaChartLayer
import com.kylecorry.trail_sense.shared.views.chart.data.LineChartLayer
import com.kylecorry.trail_sense.shared.views.chart.label.HourChartLabelFormatter
import java.time.Instant


class AstroChart(private val chart: Chart) {

    private var startTime = Instant.now()

    private val sunLine = LineChartLayer(
        emptyList(),
        Resources.color(chart.context, R.color.sun)
    )

    private val moonLine = LineChartLayer(
        emptyList(),
        Color.WHITE
    )

    private val night = FullAreaChartLayer(
        0f,
        -101f,
        Resources.color(chart.context, R.color.colorSecondary)
    )

    init {
        chart.setChartBackground(AppColor.Blue.color)

        chart.configureYAxis(
            labelCount = 0,
            drawGridLines = false,
            minimum = -100f,
            maximum = 100f,
        )

        chart.configureXAxis(
            labelCount = 7,
            drawGridLines = false,
            labelFormatter = HourChartLabelFormatter(chart.context) { startTime }
        )

        chart.plot(night, moonLine, sunLine)
    }

    // TODO: Replace with bitmap layers
    fun getPoint(datasetIdx: Int, entryIdx: Int): PixelCoordinate {
        val dataset = if (datasetIdx == 1) {
            sunLine.data
        } else {
            moonLine.data
        }

        val entry = tryOrDefault(Vector2(0f, 0f)) {
            dataset[entryIdx]
        }

        val point = chart.toPixel(entry)

        return PixelCoordinate(point.x + chart.x, point.y + chart.y)
    }

    fun plot(sun: List<Reading<Float>>, moon: List<Reading<Float>>) {
        startTime = sun.firstOrNull()?.time ?: Instant.now()
        sunLine.data = Chart.getDataFromReadings(sun, startTime) { it }
        moonLine.data = Chart.getDataFromReadings(moon, startTime) { it }
    }

}