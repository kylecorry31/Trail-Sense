package com.kylecorry.trail_sense.shared.views.chart.data

import android.graphics.Path
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Vector2
import com.kylecorry.trail_sense.shared.views.chart.IChart

// TODO: Handle on click
class ScatterChartLayer(
    override val data: List<Vector2>,
    @ColorInt val color: Int,
    val radius: Float = 6f
) : ChartLayer {
    val path = Path()

    override fun draw(drawer: ICanvasDrawer, chart: IChart) {
        drawer.noStroke()
        drawer.fill(color)
        for (point in data) {
            val mapped = chart.toPixel(point)
            drawer.circle(mapped.x, mapped.y, radius)
        }
    }

    override fun onClick(drawer: ICanvasDrawer, chart: IChart, pixel: PixelCoordinate): Boolean {
        return false
    }
}