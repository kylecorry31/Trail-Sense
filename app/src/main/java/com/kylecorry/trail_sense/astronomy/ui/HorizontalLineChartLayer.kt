package com.kylecorry.trail_sense.astronomy.ui

import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.ceres.chart.IChart
import com.kylecorry.ceres.chart.data.ChartLayer
import com.kylecorry.sol.math.Vector2

class HorizontalLineChartLayer(
    initialValue: Float,
    @ColorInt initialColor: Int,
    private val thickness: Float = 2f,
) : ChartLayer {

    @ColorInt
    var color: Int = initialColor
        set(value) {
            field = value
            invalidate()
        }

    var value: Float = initialValue
        set(value) {
            field = value
            invalidate()
        }

    override var data: List<Vector2> = emptyList()

    override var hasChanges: Boolean = false
        private set

    override fun draw(drawer: ICanvasDrawer, chart: IChart) {
        val left = chart.toPixel(Vector2(chart.xRange.start, value))
        val right = chart.toPixel(Vector2(chart.xRange.end, value))

        drawer.strokeWeight(thickness)
        drawer.stroke(color)
        drawer.line(left.x, left.y, right.x, right.y)

        // Reset the opacity
        drawer.opacity(255)

        hasChanges = false
    }

    override fun onClick(drawer: ICanvasDrawer, chart: IChart, pixel: PixelCoordinate): Boolean {
        return false
    }

    override fun invalidate() {
        hasChanges = true
    }
}