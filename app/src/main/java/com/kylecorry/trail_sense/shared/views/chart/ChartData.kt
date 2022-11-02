package com.kylecorry.trail_sense.shared.views.chart

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.sol.math.Vector2

interface ChartData {
    val data: List<Vector2>

    fun draw(drawer: ICanvasDrawer, xMap: (Float) -> Float, yMap: (Float) -> Float)
}