package com.kylecorry.trail_sense.navigation.paths.ui.drawing

import com.kylecorry.andromeda.canvas.ICanvasDrawer

class DiamondPathLineDrawerStrategy : IPathLineDrawerStrategy {
    override fun draw(
        canvasDrawer: ICanvasDrawer,
        color: Int,
        strokeScale: Float,
        block: ICanvasDrawer.() -> Unit
    ) {
        val size = 12f / strokeScale
        val dotted = DiamondPathEffect(size, size * 2)
        canvasDrawer.apply {
            pathEffect(dotted)
            noStroke()
            fill(color)
            block()
        }
    }


}