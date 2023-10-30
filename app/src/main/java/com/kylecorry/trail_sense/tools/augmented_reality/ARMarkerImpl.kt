package com.kylecorry.trail_sense.tools.augmented_reality

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.camera.AugmentedRealityUtils
import com.kylecorry.trail_sense.shared.canvas.PixelCircle
import kotlin.math.hypot

// TODO: Is the interface even needed?
class ARMarkerImpl private constructor(
    private val position: AugmentedRealityView.HorizonCoordinate?,
    private val angularDiameter: Float?,
    private val location: Coordinate?,
    private val elevation: Float?,
    private val actualDiameter: Float?,
    private val canvasObject: CanvasObject,
    private val onFocusedFn: (() -> Boolean) = { false },
    private val onClickFn: () -> Boolean = { false }
) : ARMarker {

    override fun draw(drawer: ICanvasDrawer, anchor: PixelCircle) {
        canvasObject.draw(drawer, anchor)
    }

    override fun getAngularDiameter(view: AugmentedRealityView): Float {
        if (actualDiameter != null && location != null) {
            val distance = hypot(
                view.location.distanceTo(location),
                (elevation ?: view.altitude) - view.altitude
            )
            return AugmentedRealityUtils.getAngularSize(actualDiameter, distance)
        }
        return angularDiameter ?: 1f
    }

    override fun getHorizonCoordinate(view: AugmentedRealityView): AugmentedRealityView.HorizonCoordinate {
        if (location != null) {
            return AugmentedRealityUtils.getHorizonCoordinate(
                view.location,
                view.altitude,
                location,
                elevation
            )
        }

        return position ?: AugmentedRealityView.HorizonCoordinate(0f, 0f)
    }


    override fun onFocused(): Boolean {
        return onFocusedFn()
    }

    override fun onClick(): Boolean {
        return onClickFn()
    }

    companion object {
        fun horizon(
            position: AugmentedRealityView.HorizonCoordinate?,
            angularDiameter: Float = 12f,
            canvasObject: CanvasObject,
            onFocusedFn: (() -> Boolean) = { false },
            onClickFn: () -> Boolean = { false }
        ): ARMarker {
            return ARMarkerImpl(
                position,
                angularDiameter,
                null,
                null,
                null,
                canvasObject,
                onFocusedFn,
                onClickFn
            )
        }

        fun geographic(
            location: Coordinate,
            elevation: Float,
            actualDiameter: Float,
            canvasObject: CanvasObject,
            onFocusedFn: (() -> Boolean) = { false },
            onClickFn: () -> Boolean = { false }
        ): ARMarker {
            return ARMarkerImpl(
                null,
                null,
                location,
                elevation,
                actualDiameter,
                canvasObject,
                onFocusedFn,
                onClickFn
            )
        }
    }
}