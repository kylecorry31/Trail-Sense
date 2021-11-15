package com.kylecorry.trail_sense.navigation.paths.ui.drawing

import com.kylecorry.andromeda.canvas.ArrowPathEffect
import com.kylecorry.andromeda.canvas.CanvasView

class ArrowPathLineDrawerStrategy : IPathLineDrawerStrategy {
    override fun draw(
        canvas: CanvasView,
        color: Int,
        strokeScale: Float,
        block: CanvasView.() -> Unit
    ) {
        val arrow = ArrowPathEffect(6f / strokeScale, 10f / strokeScale)
        canvas.apply {
            pathEffect(arrow)
            noStroke()
            fill(color)
            block()
        }
    }
}