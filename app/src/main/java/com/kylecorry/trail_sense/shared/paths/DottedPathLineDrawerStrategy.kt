package com.kylecorry.trail_sense.shared.paths

import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.canvas.DottedPathEffect
import com.kylecorry.trailsensecore.domain.pixels.PixelLine

class DottedPathLineDrawerStrategy: IPathLineDrawerStrategy {
    override fun drawLine(canvas: CanvasView, line: PixelLine, strokeScale: Float) {
        val dotted = DottedPathEffect(3f / strokeScale, 10f / strokeScale)
        canvas.apply {
            pathEffect(dotted)
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