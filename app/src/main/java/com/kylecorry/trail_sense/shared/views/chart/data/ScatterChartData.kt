package com.kylecorry.trail_sense.shared.views.chart.data

import android.graphics.Path
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.sol.math.Vector2

// TODO: Handle on click
class ScatterChartData(
    override val data: List<Vector2>,
    @ColorInt val color: Int,
    val radius: Float = 6f
) : ChartData {
    val path = Path()

    override fun draw(drawer: ICanvasDrawer, mapX: (Float) -> Float, mapY: (Float) -> Float) {
        drawer.noStroke()
        drawer.fill(color)
        for (point in data) {
            drawer.circle(mapX(point.x), mapY(point.y), radius)
        }
    }
}