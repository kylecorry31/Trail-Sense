package com.kylecorry.trail_sense.tools.augmented_reality

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.camera.AugmentedRealityUtils
import com.kylecorry.trail_sense.shared.canvas.PixelCircle
import com.kylecorry.trail_sense.tools.augmented_reality.position.ARPositionStrategy
import com.kylecorry.trail_sense.tools.augmented_reality.position.GeographicPositionStrategy
import com.kylecorry.trail_sense.tools.augmented_reality.position.SphericalPositionStrategy
import kotlin.math.hypot

// TODO: Are the helper methods needed, or can the constructor be called directly?
class ARMarkerImpl private constructor(
    private val positionStrategy: ARPositionStrategy,
    private val canvasObject: CanvasObject,
    private val keepFacingUp: Boolean = false,
    private val onFocusedFn: (() -> Boolean) = { false },
    private val onClickFn: () -> Boolean = { false }
) : ARMarker {

    override fun draw(view: AugmentedRealityView, drawer: ICanvasDrawer, area: PixelCircle) {
        drawer.push()
        if (keepFacingUp) {
            drawer.rotate(view.sideInclination, area.center.x, area.center.y)
        }
        canvasObject.draw(drawer, area)
        drawer.pop()
    }

    override fun getAngularDiameter(view: AugmentedRealityView): Float {
        return positionStrategy.getAngularDiameter(view)
    }

    override fun getHorizonCoordinate(view: AugmentedRealityView): AugmentedRealityView.HorizonCoordinate {
        return positionStrategy.getHorizonCoordinate(view)
    }


    override fun onFocused(): Boolean {
        return onFocusedFn()
    }

    override fun onClick(): Boolean {
        return onClickFn()
    }

    companion object {
        fun horizon(
            bearing: Float,
            elevation: Float,
            distance: Float = Float.MAX_VALUE,
            isTrueNorth: Boolean = true,
            angularDiameter: Float = 12f,
            canvasObject: CanvasObject,
            keepFacingUp: Boolean = false,
            onFocusedFn: (() -> Boolean) = { false },
            onClickFn: () -> Boolean = { false }
        ): ARMarker {
            return ARMarkerImpl(
                SphericalPositionStrategy(
                    bearing,
                    elevation,
                    distance,
                    angularDiameter,
                    isTrueNorth
                ),
                canvasObject,
                keepFacingUp,
                onFocusedFn,
                onClickFn
            )
        }

        fun geographic(
            location: Coordinate,
            elevation: Float?,
            actualDiameter: Float,
            canvasObject: CanvasObject,
            keepFacingUp: Boolean = false,
            onFocusedFn: (() -> Boolean) = { false },
            onClickFn: () -> Boolean = { false }
        ): ARMarker {
            return ARMarkerImpl(
                GeographicPositionStrategy(
                    location,
                    elevation,
                    actualDiameter
                ),
                canvasObject,
                keepFacingUp,
                onFocusedFn,
                onClickFn
            )
        }
    }
}