package com.kylecorry.trail_sense.tools.augmented_reality

import android.graphics.Color
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.trail_sense.shared.canvas.PixelCircle

interface CanvasObject {
    // TODO: Use a pixel region instead of a circle
    fun draw(drawer: ICanvasDrawer, area: PixelCircle)
}

class CircleCanvasObject(
    @ColorInt
    private val color: Int,
    @ColorInt
    private val strokeColor: Int? = null,
    private val opacity: Int = 255,
    private val strokeWeight: Float = 0.5f,
) : CanvasObject {
    override fun draw(drawer: ICanvasDrawer, area: PixelCircle) {
        val size = area.radius * 2f
        drawer.noTint()
        if (strokeColor != null && strokeColor != Color.TRANSPARENT) {
            drawer.stroke(strokeColor)
            drawer.strokeWeight(drawer.dp(strokeWeight))
        } else {
            drawer.noStroke()
        }
        if (color != Color.TRANSPARENT) {
            drawer.fill(color)
            drawer.opacity(opacity)
            drawer.circle(area.center.x, area.center.y, size)
        }
    }
}