package com.kylecorry.trail_sense.navigation.paths.ui.drawing

import com.kylecorry.andromeda.canvas.ArrowPathEffect
import com.kylecorry.andromeda.canvas.ICanvasDrawer

class ArrowPathLineDrawerStrategy : IPathLineDrawerStrategy {
    override fun draw(
        canvasDrawer: ICanvasDrawer,
        color: Int,
        strokeScale: Float,
        block: ICanvasDrawer.() -> Unit
    ) {
        val arrow = ArrowPathEffect(6f / strokeScale, 10f / strokeScale)
        canvasDrawer.apply {
            pathEffect(arrow)
            noStroke()
            fill(color)
            block()
        }
    }
}