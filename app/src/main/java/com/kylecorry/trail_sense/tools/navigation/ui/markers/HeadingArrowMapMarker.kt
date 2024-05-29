package com.kylecorry.trail_sense.tools.navigation.ui.markers

import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.units.Coordinate

class HeadingArrowMapMarker(
    override val location: Coordinate,
    @ColorInt private val strokeColor: Int,
    private val heading: Float = 0f,
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
        val v1 = Vector2(size, size).rotate(heading)
        val v2 = Vector2(-size, size).rotate(heading)
        val o1 = Vector2(anchor.x, anchor.y)
        val p1 = o1.plus(v1)
        val p2 = o1.plus(v2)

        drawer.noPathEffect()
        drawer.noFill()
        drawer.stroke(strokeColor)
        drawer.strokeWeight(strokeWeight)
        drawer.line(o1.x, o1.y, p1.x, p1.y)
        drawer.line(o1.x, o1.y, p2.x, p2.y)
    }

    override fun onClick(): Boolean {
        return onClickFn()
    }
}