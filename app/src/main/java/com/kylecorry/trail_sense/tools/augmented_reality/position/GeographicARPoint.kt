package com.kylecorry.trail_sense.tools.augmented_reality.position

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.camera.AugmentedRealityUtils
import com.kylecorry.trail_sense.tools.augmented_reality.AugmentedRealityView
import kotlin.math.hypot

/**
 * A point in the AR world
 * @param location The location of the point
 * @param elevation The elevation of the point in meters, defaults to the elevation of the camera
 * @param actualDiameter The actual diameter of the point in meters, defaults to 1 meter
 */
class GeographicARPoint(
    private val location: Coordinate,
    private val elevation: Float? = null,
    private val actualDiameter: Float = 1f
) : ARPoint {
    override fun getAngularDiameter(view: AugmentedRealityView): Float {
        val distance = hypot(
            view.location.distanceTo(location),
            (elevation ?: view.altitude) - view.altitude
        )
        return AugmentedRealityUtils.getAngularSize(actualDiameter, distance)
    }

    override fun getHorizonCoordinate(view: AugmentedRealityView): AugmentedRealityView.HorizonCoordinate {
        return AugmentedRealityUtils.getHorizonCoordinate(
            view.location,
            view.altitude,
            location,
            elevation
        )
    }
}