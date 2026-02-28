package com.kylecorry.trail_sense.shared.andromeda_temp

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.views.chart.IChart
import com.kylecorry.andromeda.views.chart.data.ChartLayer
import com.kylecorry.sol.math.Vector2

class ClickChartLayer(
    private val onClick: (value: Vector2) -> Boolean
) : ChartLayer {
    override var data: List<Vector2> = emptyList()

    override var hasChanges: Boolean = false
        private set

    override fun draw(drawer: ICanvasDrawer, chart: IChart) {
        // Do nothing
    }

    override fun onClick(drawer: ICanvasDrawer, chart: IChart, pixel: PixelCoordinate): Boolean {
        return onClick(chart.toData(pixel))
    }

    override fun invalidate() {
        // Do nothing
    }
}