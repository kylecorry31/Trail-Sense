package com.kylecorry.trail_sense.shared.views.chart.data

import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Vector2
import com.kylecorry.trail_sense.shared.views.chart.IChart
import kotlin.math.abs

class FullAreaChartLayer(
    initialTop: Float,
    initialBottom: Float,
    @ColorInt initialColor: Int
) : ChartLayer {

    @ColorInt
    var color: Int = initialColor
        set(value) {
            field = value
            invalidate()
        }

    var top: Float = initialTop
        set(value) {
            field = value
            invalidate()
        }

    var bottom: Float = initialBottom
        set(value) {
            field = value
            invalidate()
        }

    override var data: List<Vector2> = emptyList()

    override var hasChanges: Boolean = false
        private set

    override fun draw(drawer: ICanvasDrawer, chart: IChart) {

        drawer.fill(color)
        drawer.noStroke()

        val topLeft = chart.toPixel(Vector2(chart.xRange.start, top))
        val bottomRight = chart.toPixel(Vector2(chart.xRange.end, bottom))

        drawer.rect(
            topLeft.x,
            topLeft.y,
            abs(bottomRight.x - topLeft.x),
            abs(bottomRight.y - topLeft.y)
        )

        hasChanges = false
    }

    override fun onClick(drawer: ICanvasDrawer, chart: IChart, pixel: PixelCoordinate): Boolean {
        return false
    }

    override fun invalidate() {
        hasChanges = true
    }
}