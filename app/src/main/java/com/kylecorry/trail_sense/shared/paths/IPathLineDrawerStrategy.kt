package com.kylecorry.trail_sense.shared.paths

import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.trail_sense.shared.canvas.PixelLine

interface IPathLineDrawerStrategy {
    fun draw(canvas: CanvasView, line: PixelLine, strokeScale: Float = 1f)
    fun draw(canvas: CanvasView, @ColorInt color: Int, strokeScale: Float = 1f, block: CanvasView.() -> Unit)
}