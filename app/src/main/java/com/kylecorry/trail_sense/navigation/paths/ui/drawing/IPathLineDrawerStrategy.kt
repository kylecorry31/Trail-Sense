package com.kylecorry.trail_sense.navigation.paths.ui.drawing

import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer

interface IPathLineDrawerStrategy {
    fun draw(canvasDrawer: ICanvasDrawer, @ColorInt color: Int, strokeScale: Float = 1f, block: ICanvasDrawer.() -> Unit)
}