package com.kylecorry.trail_sense.navigation.paths.ui.drawing

import android.graphics.DashPathEffect
import com.kylecorry.andromeda.canvas.ICanvasDrawer

class DashedPathLineDrawerStrategy : IPathLineDrawerStrategy {
    override fun draw(
        canvasDrawer: ICanvasDrawer,
        color: Int,
        strokeScale: Float,
        block: ICanvasDrawer.() -> Unit
    ) {
        val dotted = DashPathEffect(
            floatArrayOf(3f / strokeScale, 10f / strokeScale), 0f
        )
        canvasDrawer.apply {
            pathEffect(dotted)
            noFill()
            strokeWeight(4f / strokeScale)
            stroke(color)
            block()
        }
    }


}