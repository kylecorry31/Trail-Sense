package com.kylecorry.trail_sense.navigation.paths.ui.drawing

import com.kylecorry.andromeda.canvas.CanvasView

class SolidPathLineDrawerStrategy : IPathLineDrawerStrategy {
    override fun draw(
        canvas: CanvasView,
        color: Int,
        strokeScale: Float,
        block: CanvasView.() -> Unit
    ) {
        canvas.apply {
            noPathEffect()
            noFill()
            stroke(color)
            strokeWeight(6f / strokeScale)
            block()
        }
    }
}