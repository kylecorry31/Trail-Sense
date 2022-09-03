package com.kylecorry.trail_sense.navigation.ui.markers

import android.graphics.Path
import android.graphics.PathEffect
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.units.Coordinate
import kotlin.math.max

class PathMapMarker(
    override val location: Coordinate,
    private val path: Path,
    override val size: Float = 12f,
    @ColorInt private val color: Int?,
    @ColorInt private val strokeColor: Int? = null,
    private val strokeWeight: Float = 0f,
    private val pathEffect: PathEffect? = null,
    private val pathCenter: PixelCoordinate = PixelCoordinate(0f, 0f),
    private val rotation: Float? = null,
    private val onClickFn: () -> Boolean = { false }
) : MapMarker {
    override fun draw(
        drawer: ICanvasDrawer,
        anchor: PixelCoordinate,
        scale: Float,
        rotation: Float
    ) {
        val requestedSize = drawer.dp(size) * scale
        val dimensions = drawer.pathDimensions(path)
        val neededScale = requestedSize / max(dimensions.first, dimensions.second)
        drawer.push()
        if (pathEffect != null) {
            drawer.pathEffect(pathEffect)
        } else {
            drawer.noPathEffect()
        }
        if (strokeWeight > 0f && strokeColor != null) {
            drawer.strokeWeight(strokeWeight)
            drawer.stroke(strokeColor)
        } else {
            drawer.noStroke()
        }
        if (color != null) {
            drawer.fill(color)
        } else {
            drawer.noFill()
        }
        drawer.rotate(this.rotation ?: rotation, anchor.x, anchor.y)
        drawer.translate(anchor.x + pathCenter.x, anchor.y + pathCenter.y)
        drawer.scale(neededScale, neededScale)
        drawer.path(path)
        drawer.pop()
        drawer.noPathEffect()
    }

    override fun onClick(): Boolean {
        return onClickFn()
    }
}