package com.kylecorry.trail_sense.shared.views.chart

import android.graphics.Path
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.sol.math.Vector2

// TODO: Handle on click
class LineChartData(
    override val data: List<Vector2>,
    @ColorInt val color: Int,
    val thickness: Float = 6f
) : ChartData {
    val path = Path()

    override fun draw(drawer: ICanvasDrawer, xMap: (Float) -> Float, yMap: (Float) -> Float) {
        // TODO: Scale rather than recompute
        path.rewind()
        for (i in 1 until data.size) {
            if (i == 1) {
                val start = data[0]
                path.moveTo(xMap(start.x), yMap(start.y))
            }

            val next = data[i]
            path.lineTo(xMap(next.x), yMap(next.y))
        }

        drawer.noFill()
        drawer.strokeWeight(thickness)
        drawer.stroke(color)
        drawer.path(path)
    }
}