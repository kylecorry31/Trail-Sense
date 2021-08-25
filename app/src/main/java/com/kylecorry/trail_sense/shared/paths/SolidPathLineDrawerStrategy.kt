package com.kylecorry.trail_sense.shared.paths

import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.trailsensecore.domain.pixels.PixelLine

class SolidPathLineDrawerStrategy: IPathLineDrawerStrategy {
    override fun drawLine(canvas: CanvasView, line: PixelLine, strokeScale: Float) {
        canvas.apply {
            noPathEffect()
            noFill()
            opacity(line.alpha)
            stroke(line.color)
            strokeWeight(6f / strokeScale)
            line(
                line.start.x,
                line.start.y,
                line.end.x,
                line.end.y
            )
        }
    }
}