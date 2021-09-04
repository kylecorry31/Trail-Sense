package com.kylecorry.trail_sense.shared.paths

import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.trail_sense.shared.canvas.PixelLine

interface IPathLineDrawerStrategy {
    fun draw(canvas: CanvasView, line: PixelLine, strokeScale: Float = 1f)
}