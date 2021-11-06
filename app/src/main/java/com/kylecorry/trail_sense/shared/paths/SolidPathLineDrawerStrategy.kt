package com.kylecorry.trail_sense.shared.paths

import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.trail_sense.shared.canvas.PixelLine

class SolidPathLineDrawerStrategy : IPathLineDrawerStrategy {
    override fun draw(canvas: CanvasView, line: PixelLine, strokeScale: Float) {
        draw(canvas, line.color, strokeScale) {
            line(
                line.start.x,
                line.start.y,
                line.end.x,
                line.end.y
            )
        }
    }

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