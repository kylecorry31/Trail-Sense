package com.kylecorry.trail_sense.shared.andromeda_temp

import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.views.chart.IChart
import com.kylecorry.andromeda.views.chart.data.BaseChartLayer
import com.kylecorry.sol.math.Vector2
import kotlin.math.abs

// TODO: Don't cut off ends (maybe add a get bounds method to ChartLayer)
class HistogramChartLayer(
    initialData: List<Vector2>,
    @ColorInt initialColor: Int,
    val width: Float = 6f,
    private val onBarClicked: (point: Vector2) -> Boolean = { false },
) : BaseChartLayer(initialData, false) {

    @ColorInt
    var color: Int = initialColor
        set(value) {
            field = value
            invalidate()
        }

    override fun draw(drawer: ICanvasDrawer, chart: IChart) {
        drawer.noStroke()
        drawer.fill(color)
        for (point in data) {
            val topLeft = chart.toPixel(Vector2(point.x - width / 2, point.y))
            val bottomRight = chart.toPixel(Vector2(point.x + width / 2, 0f))
            drawer.rect(
                topLeft.x,
                topLeft.y,
                abs(bottomRight.x - topLeft.x),
                abs(bottomRight.y - topLeft.y)
            )
        }
        super.draw(drawer, chart)

        // Reset the opacity
        drawer.opacity(255)
    }

    override fun onClick(drawer: ICanvasDrawer, chart: IChart, pixel: PixelCoordinate): Boolean {
        val bar = data.map {
            val topLeft = chart.toPixel(Vector2(it.x - width / 2, it.y))
            val bottomRight = chart.toPixel(Vector2(it.x + width / 2, 0f))
            Triple(it, topLeft, bottomRight)
        }.firstOrNull {
            pixel.x in it.second.x..it.third.x
        }

        if (bar != null) {
            return onBarClicked(bar.first)
        }

        return false
    }
}