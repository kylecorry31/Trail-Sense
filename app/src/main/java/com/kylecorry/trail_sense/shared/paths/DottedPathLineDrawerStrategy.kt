package com.kylecorry.trail_sense.shared.paths

import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.canvas.DottedPathEffect
import com.kylecorry.trail_sense.shared.canvas.PixelLine

class DottedPathLineDrawerStrategy : IPathLineDrawerStrategy {
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
        val dotted = DottedPathEffect(3f / strokeScale, 10f / strokeScale)
        canvas.apply {
            pathEffect(dotted)
            noStroke()
            fill(color)
            block()
        }
    }


}