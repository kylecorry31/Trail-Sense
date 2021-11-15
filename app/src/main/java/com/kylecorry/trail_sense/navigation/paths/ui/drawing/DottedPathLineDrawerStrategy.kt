package com.kylecorry.trail_sense.navigation.paths.ui.drawing

import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.canvas.DottedPathEffect

class DottedPathLineDrawerStrategy : IPathLineDrawerStrategy {
    override fun draw(
        canvas: CanvasView,
        color: Int,
        strokeScale: Float,
        block: CanvasView.() -> Unit
    ) {
        val dotted = DottedPathEffect(3f / strokeScale, 10f / strokeScale)
        canvas.apply {
            pathEffect(dotted)
            noStroke()
            fill(color)
            block()
        }
    }


}