package com.kylecorry.trail_sense.tools.augmented_reality.ui

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.trail_sense.shared.canvas.PixelCircle
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.ARPoint

class ARMarker(
    val point: ARPoint,
    private val canvasObject: CanvasObject,
    private val keepFacingUp: Boolean = false,
    private val onFocusedFn: (() -> Boolean) = { false },
    private val onClickFn: () -> Boolean = { false }
) {

    fun draw(view: AugmentedRealityView, drawer: ICanvasDrawer, area: PixelCircle) {
        drawer.push()
        if (keepFacingUp) {
            drawer.rotate(view.sideInclination, area.center.x, area.center.y)
        }
        canvasObject.draw(drawer, area)
        drawer.pop()
    }

    /**
     * Gets the location of the marker on the screen
     * @param view The AR view
     * @return The location of the marker
     */
    fun getViewLocation(view: AugmentedRealityView): PixelCircle {
        val coordinates = point.getAugmentedRealityCoordinate(view)
        val angularDiameter = point.getAngularDiameter(view)
        val diameter = view.sizeToPixel(angularDiameter)
        return PixelCircle(
            view.toPixel(coordinates),
            diameter / 2f
        )
    }

    fun onFocused(): Boolean {
        return onFocusedFn()
    }

    fun onClick(): Boolean {
        return onClickFn()
    }
}