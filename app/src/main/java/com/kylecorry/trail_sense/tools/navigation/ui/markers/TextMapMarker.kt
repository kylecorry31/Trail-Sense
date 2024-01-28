package com.kylecorry.trail_sense.tools.navigation.ui.markers

import android.graphics.Color
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.units.Coordinate

class TextMapMarker(
    override val location: Coordinate,
    private val text: String,
    @ColorInt private val color: Int,
    @ColorInt private val strokeColor: Int? = null,
    private val opacity: Int = 255,
    override val size: Float = 12f,
    private val strokeWeight: Float = 0.5f,
    private val rotation: Float? = null,
    private val onClickFn: () -> Boolean = { false }
) : MapMarker {
    override fun draw(
        drawer: ICanvasDrawer,
        anchor: PixelCoordinate,
        scale: Float,
        rotation: Float
    ) {
        val size = drawer.sp(this.size)
        drawer.noTint()
        if (strokeColor != null && strokeColor != Color.TRANSPARENT) {
            drawer.stroke(strokeColor)
            drawer.strokeWeight(drawer.dp(strokeWeight) * scale)
        } else {
            drawer.noStroke()
        }
        if (color != Color.TRANSPARENT) {
            drawer.fill(color)
            drawer.opacity(opacity)
            drawer.textSize(size * scale)
            drawer.textMode(TextMode.Center)
            drawer.push()
            drawer.rotate(this.rotation ?: rotation, anchor.x, anchor.y)
            drawer.text(text, anchor.x, anchor.y)
            drawer.pop()
            drawer.textMode(TextMode.Corner)
        }
    }

    override fun onClick(): Boolean {
        return onClickFn()
    }
}