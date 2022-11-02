package com.kylecorry.trail_sense.shared.views.chart.data

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.sol.math.Vector2

interface ChartData {
    val data: List<Vector2>

    fun draw(drawer: ICanvasDrawer, mapX: (Float) -> Float, mapY: (Float) -> Float)
}