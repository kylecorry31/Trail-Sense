package com.kylecorry.trail_sense.shared.paths

import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.trailsensecore.domain.pixels.PixelLine

interface IPathLineDrawerStrategy {
    fun drawLine(canvas: CanvasView, line: PixelLine, strokeScale: Float = 1f)
}