package com.kylecorry.trail_sense.shared.paths

import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.trail_sense.shared.AppColor
import com.kylecorry.trail_sense.shared.canvas.PixelLine

class GrayPathLineDrawerDecoratorStrategy(private val drawer: IPathLineDrawerStrategy) :
    IPathLineDrawerStrategy {
    override fun draw(canvas: CanvasView, line: PixelLine, strokeScale: Float) {
        drawer.draw(canvas, line.copy(color = AppColor.Gray.color), strokeScale)
    }
}