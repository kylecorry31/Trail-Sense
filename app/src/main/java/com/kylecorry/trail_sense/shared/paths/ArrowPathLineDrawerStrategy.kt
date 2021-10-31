package com.kylecorry.trail_sense.shared.paths

import com.kylecorry.andromeda.canvas.ArrowPathEffect
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.trail_sense.shared.canvas.PixelLine

class ArrowPathLineDrawerStrategy : IPathLineDrawerStrategy {
    override fun draw(canvas: CanvasView, line: PixelLine, strokeScale: Float) {
        val arrow = ArrowPathEffect(6f / strokeScale, 10f / strokeScale)
        canvas.apply {
            pathEffect(arrow)
            noStroke()
            fill(line.color)
            opacity(line.alpha)
            line(
                line.start.x,
                line.start.y,
                line.end.x,
                line.end.y
            )
        }
    }
}