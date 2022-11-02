package com.kylecorry.trail_sense.shared.views.chart

import android.graphics.Path
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.sol.math.Vector2

class BoundsChartData(
    val upper: List<Vector2>,
    val lower: List<Vector2>,
    @ColorInt val color: Int
) : ChartData {

    override val data: List<Vector2> = upper + lower

    val path = Path()

    override fun draw(drawer: ICanvasDrawer, xMap: (Float) -> Float, yMap: (Float) -> Float) {
        // TODO: Scale rather than recompute
        path.rewind()
        // Add upper to path
        for (i in 1 until upper.size) {
            if (i == 1) {
                val start = upper[0]
                path.moveTo(xMap(start.x), yMap(start.y))
            }

            val next = upper[i]
            path.lineTo(xMap(next.x), yMap(next.y))
        }

        // Add lower to path
        for (i in (0..lower.lastIndex).reversed()) {
            val next = lower[i]
            path.lineTo(xMap(next.x), yMap(next.y))
        }

        path.close()

        drawer.fill(color)
        drawer.noStroke()
        drawer.path(path)
    }
}