package com.kylecorry.trail_sense.shared.views.chart.data

import android.graphics.Path
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Vector2
import com.kylecorry.trail_sense.shared.views.chart.IChart

// TODO: Handle on click
class LineChartLayer(
    override val data: List<Vector2>,
    @ColorInt val color: Int,
    val thickness: Float = 6f
) : ChartLayer {
    val path = Path()

    override fun draw(drawer: ICanvasDrawer, chart: IChart) {
        // TODO: Scale rather than recompute
        path.rewind()
        for (i in 1 until data.size) {
            if (i == 1) {
                val start = chart.toPixel(data[0])
                path.moveTo(start.x, start.y)
            }

            val next = chart.toPixel(data[i])
            path.lineTo(next.x, next.y)
        }

        drawer.noFill()
        drawer.strokeWeight(thickness)
        drawer.stroke(color)
        drawer.path(path)
    }

    override fun onClick(drawer: ICanvasDrawer, chart: IChart, pixel: PixelCoordinate): Boolean {
        return false
    }
}