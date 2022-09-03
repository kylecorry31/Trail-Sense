package com.kylecorry.trail_sense.navigation.ui.markers

import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.units.Coordinate

class CircleMapMarker(
    override val location: Coordinate,
    @ColorInt private val color: Int,
    @ColorInt private val strokeColor: Int? = null,
    private val opacity: Int = 255,
    override val size: Float = 12f,
    private val strokeWeight: Float = 0.5f,
    private val onClickFn: () -> Boolean = { false }
) : MapMarker {
    override fun draw(
        drawer: ICanvasDrawer,
        anchor: PixelCoordinate,
        scale: Float,
        rotation: Float
    ) {
        val size = drawer.dp(this.size)
        drawer.noTint()
        if (strokeColor != null) {
            drawer.stroke(strokeColor)
            drawer.strokeWeight(drawer.dp(strokeWeight) * scale)
        } else {
            drawer.noStroke()
        }
        drawer.fill(color)
        drawer.opacity(opacity)
        drawer.circle(anchor.x, anchor.y, size * scale)
    }

    override fun onClick(): Boolean {
        return onClickFn()
    }
}