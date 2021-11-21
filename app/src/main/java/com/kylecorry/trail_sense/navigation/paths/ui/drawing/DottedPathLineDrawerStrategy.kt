package com.kylecorry.trail_sense.navigation.paths.ui.drawing

import com.kylecorry.andromeda.canvas.DottedPathEffect
import com.kylecorry.andromeda.canvas.ICanvasDrawer

class DottedPathLineDrawerStrategy : IPathLineDrawerStrategy {
    override fun draw(
        canvasDrawer: ICanvasDrawer,
        color: Int,
        strokeScale: Float,
        block: ICanvasDrawer.() -> Unit
    ) {
        val dotted = DottedPathEffect(3f / strokeScale, 10f / strokeScale)
        canvasDrawer.apply {
            pathEffect(dotted)
            noStroke()
            fill(color)
            block()
        }
    }


}