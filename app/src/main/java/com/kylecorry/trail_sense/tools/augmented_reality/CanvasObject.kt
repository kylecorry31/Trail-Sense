package com.kylecorry.trail_sense.tools.augmented_reality

import android.graphics.Color
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.ImageMode
import com.kylecorry.trail_sense.shared.canvas.PixelCircle

// TODO: Use this on the MapView as well - maybe extract to Andromeda

interface CanvasObject {
    fun draw(drawer: ICanvasDrawer, area: PixelCircle)
}

class CanvasCircle(
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

class CanvasBitmap(
    private val bitmap: android.graphics.Bitmap,
    private val scale: Float = 1f,
    private val opacity: Int = 255,
    private val tint: Int? = null
) : CanvasObject {

    private val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()

    override fun draw(drawer: ICanvasDrawer, area: PixelCircle) {
        if (tint != null) {
            drawer.tint(tint)
        } else {
            drawer.noTint()
        }
        drawer.opacity(opacity)

        // Choose the maximum width and height that fit in the circle
        val width = area.radius * 2f * scale
        val height = width / aspectRatio
        drawer.imageMode(ImageMode.Center)
        drawer.image(
            bitmap,
            area.center.x,
            area.center.y,
            width,
            height
        )

        drawer.noTint()
    }
}