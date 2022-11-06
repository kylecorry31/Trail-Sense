package com.kylecorry.trail_sense.shared.views.chart.data

import android.graphics.Path
import androidx.annotation.ColorInt
import androidx.core.graphics.alpha
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Vector2
import com.kylecorry.trail_sense.shared.views.chart.IChart

class AreaChartLayer(
    initialData: List<Vector2>,
    @ColorInt initialLineColor: Int,
    @ColorInt initialAreaColor: Int,
    initialFillTo: Float = 0f,
    private val lineThickness: Float = 2f,
    onPointClick: (point: Vector2) -> Boolean = { false }
) : BaseChartLayer(initialData, true, onPointClick = onPointClick) {

    @ColorInt
    var lineColor: Int = initialLineColor
        set(value) {
            field = value
            invalidate()
        }

    @ColorInt
    var areaColor: Int = initialAreaColor
        set(value) {
            field = value
            invalidate()
        }

    var fillTo: Float = initialFillTo
        set(value) {
            field = value
            invalidate()
        }

    val areaPath = Path()
    val linePath = Path()

    override fun draw(drawer: ICanvasDrawer, chart: IChart) {
        if (hasChanges) {

            if (lineColor.alpha != 0) {
                // Top line
                linePath.rewind()
                for (i in 1 until data.size) {
                    if (i == 1) {
                        val start = chart.toPixel(data[0])
                        linePath.moveTo(start.x, start.y)
                    }

                    val next = chart.toPixel(data[i])
                    linePath.lineTo(next.x, next.y)
                }
            }

            // Area
            areaPath.rewind()
            // Add upper to path
            for (i in 1 until data.size) {
                if (i == 1) {
                    val start = chart.toPixel(data[0])
                    areaPath.moveTo(start.x, start.y)
                }

                val next = chart.toPixel(data[i])
                areaPath.lineTo(next.x, next.y)
            }

            // Add fill to
            val fillToPoints =
                listOfNotNull(
                    data.lastOrNull()?.copy(y = fillTo),
                    data.firstOrNull()?.copy(y = fillTo)
                )
            for (point in fillToPoints) {
                val next = chart.toPixel(point)
                areaPath.lineTo(next.x, next.y)
            }

            areaPath.close()

        }

        if (lineColor.alpha != 0) {
            drawer.noFill()
            drawer.strokeWeight(drawer.dp(lineThickness))
            drawer.stroke(lineColor)
            drawer.path(linePath)
        }

        drawer.fill(areaColor)
        drawer.noStroke()
        drawer.path(areaPath)

        super.draw(drawer, chart)
    }

    override fun onClick(drawer: ICanvasDrawer, chart: IChart, pixel: PixelCoordinate): Boolean {
        return false
    }
}