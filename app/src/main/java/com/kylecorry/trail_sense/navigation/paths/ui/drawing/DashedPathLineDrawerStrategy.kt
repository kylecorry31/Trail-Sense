package com.kylecorry.trail_sense.navigation.paths.ui.drawing

import android.graphics.DashPathEffect
import com.kylecorry.andromeda.canvas.CanvasView

class DashedPathLineDrawerStrategy : IPathLineDrawerStrategy {
    override fun draw(
        canvas: CanvasView,
        color: Int,
        strokeScale: Float,
        block: CanvasView.() -> Unit
    ) {
        val dotted = DashPathEffect(
            floatArrayOf(3f / strokeScale, 10f / strokeScale), 0f
        )
        canvas.apply {
            pathEffect(dotted)
            noFill()
            strokeWeight(4f / strokeScale)
            stroke(color)
            block()
        }
    }


}