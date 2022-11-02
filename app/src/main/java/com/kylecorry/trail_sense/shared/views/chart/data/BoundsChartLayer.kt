package com.kylecorry.trail_sense.shared.views.chart.data

import android.graphics.Path
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Vector2
import com.kylecorry.trail_sense.shared.views.chart.IChart

class BoundsChartLayer(
    private val upper: List<Vector2>,
    private val lower: List<Vector2>,
    @ColorInt private val color: Int
) : ChartLayer {

    override val data: List<Vector2> = upper + lower

    val path = Path()

    override fun draw(drawer: ICanvasDrawer, chart: IChart) {
        // TODO: Scale rather than recompute
        path.rewind()
        // Add upper to path
        for (i in 1 until upper.size) {
            if (i == 1) {
                val start = chart.toPixel(upper[0])
                path.moveTo(start.x, start.y)
            }

            val next = chart.toPixel(upper[i])
            path.lineTo(next.x, next.y)
        }

        // Add lower to path
        for (i in (0..lower.lastIndex).reversed()) {
            val next = chart.toPixel(lower[i])
            path.lineTo(next.x, next.y)
        }

        path.close()

        drawer.fill(color)
        drawer.noStroke()
        drawer.path(path)
    }

    override fun onClick(drawer: ICanvasDrawer, chart: IChart, pixel: PixelCoordinate): Boolean {
        return false
    }
}