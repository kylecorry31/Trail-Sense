package com.kylecorry.trail_sense.navigation.paths.ui.drawing

import com.kylecorry.andromeda.canvas.ICanvasDrawer

class SquarePathLineDrawerStrategy : IPathLineDrawerStrategy {
    override fun draw(
        canvasDrawer: ICanvasDrawer,
        color: Int,
        strokeScale: Float,
        block: ICanvasDrawer.() -> Unit
    ) {
        val size = 8f / strokeScale
        val dotted = SquarePathEffect(size, size * 2)
        canvasDrawer.apply {
            pathEffect(dotted)
            noStroke()
            fill(color)
            block()
        }
    }


}