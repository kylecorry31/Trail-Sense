package com.kylecorry.trail_sense.tools.navigation.ui.markers

import android.graphics.Color
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.SizeUnit

class CircleMapMarker(
    override val location: Coordinate?,
    @ColorInt private val color: Int,
    @ColorInt private val strokeColor: Int? = null,
    private val opacity: Int = 255,
    override val size: Float = 12f,
    private val strokeWeight: Float = 0.5f,
    private val sizeUnit: SizeUnit = SizeUnit.DensityPixels,
    private val useScale: Boolean = true,
    override val scaleToLocationAccuracy: Boolean = false,
    private val onClickFn: () -> Boolean = { false }
) : MapMarker {
    override val rotation: Float? = null
    override val rotateWithUser: Boolean = false
    override fun draw(
        drawer: ICanvasDrawer,
        anchor: PixelCoordinate,
        scale: Float,
        rotation: Float,
        metersPerPixel: Float,
    ) {
        val actualScale = if (useScale) scale else 1f
        val size = calculateSizeInPixels(drawer, metersPerPixel, actualScale)
        drawer.noTint()
        if (strokeColor != null && strokeColor != Color.TRANSPARENT) {
            drawer.stroke(strokeColor)
            drawer.strokeWeight(drawer.dp(strokeWeight) * actualScale)
        } else {
            drawer.noStroke()
        }
        if (color != Color.TRANSPARENT) {
            drawer.fill(color)
            drawer.opacity(opacity)
            drawer.circle(anchor.x, anchor.y, size)
            drawer.opacity(255)
        }
    }

    override fun onClick(): Boolean {
        return onClickFn()
    }

    override fun calculateSizeInPixels(
        drawer: ICanvasDrawer,
        metersPerPixel: Float,
        scale: Float
    ): Float {
        return when (sizeUnit) {
            SizeUnit.DensityPixels -> {
                drawer.dp(this.size)
            }

            SizeUnit.Meters -> {
                this.size / metersPerPixel
            }

            else -> {
                this.size
            }
        } * (if (useScale) scale else 1f)
    }
}