package com.kylecorry.trail_sense.shared.views.chart.data

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Vector2
import com.kylecorry.trail_sense.shared.canvas.PixelCircle
import com.kylecorry.trail_sense.shared.views.chart.IChart

abstract class BaseChartLayer(
    override val data: List<Vector2>,
    private val handleClicks: Boolean = true,
    private val pointClickRadiusPixels: Float = 12f,
    private val onPointClick: (point: Vector2) -> Boolean = { false },
) : ChartLayer {

    override fun onClick(drawer: ICanvasDrawer, chart: IChart, pixel: PixelCoordinate): Boolean {
        if (!handleClicks) {
            return false
        }
        val clicked = data.map {
            val anchor = chart.toPixel(it)
            it to PixelCircle(anchor, pointClickRadiusPixels)
        }
            .filter { it.second.contains(pixel) }
            .sortedBy { it.second.center.distanceTo(pixel) }

        for (point in clicked) {
            if (onPointClick(point.first)) {
                return true
            }
        }
        return false
    }
}