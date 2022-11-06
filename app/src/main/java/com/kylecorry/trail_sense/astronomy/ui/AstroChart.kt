package com.kylecorry.trail_sense.astronomy.ui

import android.graphics.Color
import androidx.annotation.DrawableRes
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.ui.BitmapLoader
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.views.chart.Chart
import com.kylecorry.trail_sense.shared.views.chart.data.BitmapChartLayer
import com.kylecorry.trail_sense.shared.views.chart.data.FullAreaChartLayer
import com.kylecorry.trail_sense.shared.views.chart.data.LineChartLayer
import com.kylecorry.trail_sense.shared.views.chart.label.HourChartLabelFormatter
import java.time.Instant


class AstroChart(private val chart: Chart, private val onImageClick: () -> Unit) {

    private var startTime = Instant.now()

    private val bitmapLoader = BitmapLoader(chart.context)

    private var previousMoonImage = R.drawable.ic_moon

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
        -100f,
        Resources.color(chart.context, R.color.colorSecondary)
    )

    private val imageSize = Resources.dp(chart.context, 24f)

    private val sunImage = BitmapChartLayer(
        emptyList(),
        bitmapLoader.load(R.drawable.ic_sun, imageSize.toInt()),
        24f
    ) {
        onImageClick()
        true
    }

    private val moonImage = BitmapChartLayer(
        emptyList(),
        bitmapLoader.load(R.drawable.ic_moon, imageSize.toInt()),
        24f
    ) {
        onImageClick()
        true
    }

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

        chart.plot(night, moonLine, sunLine, moonImage, sunImage)
    }

    fun plot(sun: List<Reading<Float>>, moon: List<Reading<Float>>) {
        startTime = sun.firstOrNull()?.time ?: Instant.now()
        sunLine.data = Chart.getDataFromReadings(sun, startTime) { it }
        moonLine.data = Chart.getDataFromReadings(moon, startTime) { it }
    }

    fun moveSun(position: Reading<Float>?) {
        sunImage.data = if (position == null) {
            emptyList()
        } else {
            Chart.getDataFromReadings(listOf(position), startTime) { it }
        }
    }

    fun setMoonImage(@DrawableRes icon: Int) {
        moonImage.bitmap = bitmapLoader.load(icon, imageSize.toInt())
        if (icon != previousMoonImage) {
            bitmapLoader.unload(previousMoonImage)
        }
        previousMoonImage = icon
    }

    fun moveMoon(position: Reading<Float>?) {
        moonImage.data = if (position == null) {
            emptyList()
        } else {
            Chart.getDataFromReadings(listOf(position), startTime) { it }
        }
    }

}