package com.kylecorry.trail_sense.navigation.paths.ui.drawing

import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.CanvasView

interface IPathLineDrawerStrategy {
    fun draw(canvas: CanvasView, @ColorInt color: Int, strokeScale: Float = 1f, block: CanvasView.() -> Unit)
}