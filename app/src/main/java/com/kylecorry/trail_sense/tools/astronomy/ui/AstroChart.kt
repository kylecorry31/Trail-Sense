package com.kylecorry.trail_sense.tools.astronomy.ui

import android.graphics.Color
import androidx.annotation.DrawableRes
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.andromeda.views.chart.Chart
import com.kylecorry.andromeda.views.chart.data.AreaChartLayer
import com.kylecorry.andromeda.views.chart.data.BitmapChartLayer
import com.kylecorry.andromeda.views.chart.data.FullAreaChartLayer
import com.kylecorry.andromeda.views.chart.data.HorizontalLineChartLayer
import com.kylecorry.andromeda.views.chart.data.LineChartLayer
import com.kylecorry.andromeda.views.chart.data.TextChartLayer
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.colors.ColorUtils
import com.kylecorry.trail_sense.shared.views.chart.label.HourChartLabelFormatter
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.navigation.ui.BitmapLoader
import java.time.Instant


class AstroChart(private val chart: Chart, private val onImageClick: () -> Unit) {

    private var startTime = Instant.now()

    private val bitmapLoader = BitmapLoader(chart.context)

    private var previousMoonImage = R.drawable.ic_moon

    private val sunLine = LineChartLayer(
        emptyList(),
        Resources.color(chart.context, R.color.sun),
        2.5f
    )

    private val sunArea = AreaChartLayer(
        emptyList(),
        Color.TRANSPARENT,
        Resources.color(chart.context, R.color.sun).withAlpha(50),
        0f
    )

    private val moonLine = LineChartLayer(
        emptyList(),
        Resources.androidTextColorSecondary(chart.context).withAlpha(100),
        1f
    )

    private val moonArea = AreaChartLayer(
        emptyList(),
        Color.TRANSPARENT,
        Resources.androidTextColorSecondary(chart.context).withAlpha(10),
        0f
    )

    private val lineColor = Resources.color(chart.context, R.color.colorSecondary).withAlpha(100)

    private val horizon = HorizontalLineChartLayer(
        0f,
        lineColor,
        2f
    )

    private val horizonLabel = TextChartLayer(
        chart.context.getString(R.string.horizon),
        Vector2(0f, 5f),
        Resources.androidTextColorSecondary(chart.context).withAlpha(100),
        10f,
        TextChartLayer.TextVerticalPosition.Bottom,
        TextChartLayer.TextHorizontalPosition.Right
    )

    private val nightColor = ColorUtils.backgroundColor(chart.context)

    private val night = FullAreaChartLayer(
        0f,
        MIN_Y,
        nightColor.withAlpha(180)
    )

    private val civilDarkness = FullAreaChartLayer(
        0f,
        MIN_Y,
        nightColor.withAlpha(127)
    )

    private val nauticalDarkness = FullAreaChartLayer(
        AstronomyService.SUN_MIN_ALTITUDE_CIVIL,
        MIN_Y,
        nightColor.withAlpha(64)
    )

    private val astronomicalDarkness = FullAreaChartLayer(
        AstronomyService.SUN_MIN_ALTITUDE_NAUTICAL,
        MIN_Y,
        nightColor.withAlpha(32)
    )

    private val fullDarkness = FullAreaChartLayer(
        AstronomyService.SUN_MIN_ALTITUDE_ASTRONOMICAL,
        MIN_Y,
        nightColor.withAlpha(16)
    )

    private val civilLine = HorizontalLineChartLayer(
        AstronomyService.SUN_MIN_ALTITUDE_CIVIL,
        lineColor,
        2f
    )

    private val nauticalLine = HorizontalLineChartLayer(
        AstronomyService.SUN_MIN_ALTITUDE_NAUTICAL,
        lineColor,
        2f
    )

    private val astronomicalLine = HorizontalLineChartLayer(
        AstronomyService.SUN_MIN_ALTITUDE_ASTRONOMICAL,
        lineColor,
        2f
    )

    private var showBands = false

    private val imageSize = Resources.dp(chart.context, 24f)

    private val sunImage = BitmapChartLayer(
        emptyList(),
        bitmapLoader.load(R.drawable.ic_sun, imageSize.toInt()),
        16f,
    ) {
        onImageClick()
        true
    }

    private val moonImage = BitmapChartLayer(
        emptyList(),
        bitmapLoader.load(R.drawable.ic_moon, imageSize.toInt()),
        16f
    ) {
        onImageClick()
        true
    }

    init {
        chart.configureYAxis(
            labelCount = 0,
            drawGridLines = false,
            minimum = MIN_Y,
            maximum = 100f
        )

        chart.configureXAxis(
            labelCount = 7,
            drawGridLines = false,
            labelFormatter = HourChartLabelFormatter(chart.context) { startTime }
        )

        chart.emptyText = chart.context.getString(R.string.no_data)

        updateLayers()

        chart.setShouldRerenderEveryCycle(false)
    }

    fun setBands(show: Boolean) {
        if (show == showBands) return
        showBands = show
        updateLayers()
    }

    private fun updateLayers() {
        val layers = mutableListOf(
            horizon,
            horizonLabel,
            moonArea,
            sunArea,
            moonLine,
            sunLine,
            moonImage,
            sunImage
        )

        if (showBands) {
            layers.add(civilDarkness)
            layers.add(nauticalDarkness)
            layers.add(astronomicalDarkness)
            layers.add(fullDarkness)
            layers.add(0, civilLine)
            layers.add(0, nauticalLine)
            layers.add(0, astronomicalLine)
        } else {
            layers.add(night)
        }

        chart.plot(layers)
    }

    fun plot(sun: List<Reading<Float>>, moon: List<Reading<Float>>) {
        startTime = sun.firstOrNull()?.time ?: Instant.now()
        sunLine.data = Chart.getDataFromReadings(sun, startTime) { it }
        moonLine.data = Chart.getDataFromReadings(moon, startTime) { it }
        val endX = sunLine.data.lastOrNull()?.x ?: 0f
        horizonLabel.position = horizonLabel.position.copy(x = endX - 0.1f)
        updateSunArea()
        updateMoonArea()
        chart.invalidate()
    }

    fun moveSun(position: Reading<Float>?) {
        sunImage.data = if (position == null) {
            emptyList()
        } else {
            Chart.getDataFromReadings(listOf(position), startTime) { it }
        }
        updateSunArea()
        chart.invalidate()
    }

    fun setMoonImage(@DrawableRes icon: Int) {
        moonImage.bitmap = bitmapLoader.load(icon, imageSize.toInt())
        if (icon != previousMoonImage) {
            bitmapLoader.unload(previousMoonImage)
        }
        previousMoonImage = icon
        chart.invalidate()
    }

    fun moveMoon(position: Reading<Float>?, tilt: Float? = null) {
        moonImage.data = if (position == null) {
            emptyList()
        } else {
            Chart.getDataFromReadings(listOf(position), startTime) { it }
        }
        moonImage.rotation = tilt ?: 0f
        updateMoonArea()
        chart.invalidate()
    }

    private fun updateSunArea() {
        val position = sunImage.data.firstOrNull()
        if (position == null) {
            sunArea.data = emptyList()
        } else {
            sunArea.data = sunLine.data.filter { it.x <= position.x } + position
        }
    }

    private fun updateMoonArea() {
        val position = moonImage.data.firstOrNull()
        if (position == null) {
            moonArea.data = emptyList()
        } else {
            moonArea.data = moonLine.data.filter { it.x <= position.x } + position
        }
    }

    companion object {
        private const val MIN_Y = -100f
    }
}