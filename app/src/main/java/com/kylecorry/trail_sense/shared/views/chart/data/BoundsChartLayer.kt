package com.kylecorry.trail_sense.shared.views.chart.data

import android.graphics.Path
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Vector2
import com.kylecorry.trail_sense.shared.views.chart.IChart

class BoundsChartLayer(
    initialUpper: List<Vector2>,
    initialLower: List<Vector2>,
    @ColorInt initialColor: Int
) : ChartLayer {

    @ColorInt
    var color: Int = initialColor
        set(value) {
            field = value
            invalidate()
        }

    var upper: List<Vector2> = initialUpper
        set(value) {
            field = value
            invalidate()
        }

    var lower: List<Vector2> = initialLower
        set(value) {
            field = value
            invalidate()
        }

    override var data: List<Vector2>
        get() = upper + lower
        set(value) {
            // Do nothing - eventually maybe determine which is upper and which is lower
        }

    override var hasChanges: Boolean = false
        private set

    val path = Path()

    override fun draw(drawer: ICanvasDrawer, chart: IChart) {
        if (hasChanges) {
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
        }

        drawer.fill(color)
        drawer.noStroke()
        drawer.path(path)

        hasChanges = false
    }

    override fun onClick(drawer: ICanvasDrawer, chart: IChart, pixel: PixelCoordinate): Boolean {
        return false
    }

    override fun invalidate() {
        hasChanges = true
    }
}