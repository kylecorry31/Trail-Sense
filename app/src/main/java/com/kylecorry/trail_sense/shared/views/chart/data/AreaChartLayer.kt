package com.kylecorry.trail_sense.shared.views.chart.data

import android.graphics.Path
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Vector2
import com.kylecorry.trail_sense.shared.views.chart.IChart

class AreaChartLayer(
    override val data: List<Vector2>,
    @ColorInt val color: Int,
    val fillTo: Float = 0f
) : ChartLayer {

    val path = Path()

    override fun draw(drawer: ICanvasDrawer, chart: IChart) {
        // TODO: Scale rather than recompute
        path.rewind()
        // Add upper to path
        for (i in 1 until data.size) {
            if (i == 1) {
                val start = chart.toPixel(data[0])
                path.moveTo(start.x, start.y)
            }

            val next = chart.toPixel(data[i])
            path.lineTo(next.x, next.y)
        }

        // Add fill to
        val fillToPoints =
            listOfNotNull(data.lastOrNull()?.copy(y = fillTo), data.firstOrNull()?.copy(y = fillTo))
        for (point in fillToPoints) {
            val next = chart.toPixel(point)
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