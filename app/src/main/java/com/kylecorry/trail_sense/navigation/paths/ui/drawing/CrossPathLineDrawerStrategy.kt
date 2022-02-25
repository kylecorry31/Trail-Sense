package com.kylecorry.trail_sense.navigation.paths.ui.drawing

import com.kylecorry.andromeda.canvas.ICanvasDrawer

class CrossPathLineDrawerStrategy : IPathLineDrawerStrategy {
    override fun draw(
        canvasDrawer: ICanvasDrawer,
        color: Int,
        strokeScale: Float,
        block: ICanvasDrawer.() -> Unit
    ) {
        val size = 12f / strokeScale
        val effect = CrossPathEffect(size, size * 2)
        canvasDrawer.apply {
            pathEffect(effect)
            noFill()
            strokeWeight(4f / strokeScale)
            stroke(color)
            block()
        }
    }


}