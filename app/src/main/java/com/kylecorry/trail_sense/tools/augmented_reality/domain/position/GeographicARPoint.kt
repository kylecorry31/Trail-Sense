package com.kylecorry.trail_sense.tools.augmented_reality.domain.position

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.camera.AugmentedRealityUtils
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView
import kotlin.math.hypot

/**
 * A point in the AR world
 * @param location The location of the point
 * @param elevation The elevation of the point in meters, defaults to the elevation of the camera
 * @param actualDiameter The actual diameter of the point in meters, defaults to 1 meter
 */
class GeographicARPoint(
    val location: Coordinate,
    val elevation: Float? = null,
    val actualDiameter: Float = 1f,
    val isElevationRelative: Boolean = false
) : ARPoint {
    override fun getAngularDiameter(view: AugmentedRealityView): Float {
        val distance = hypot(
            view.location.distanceTo(location),
            getActualElevation(view) - view.altitude
        )
        return AugmentedRealityUtils.getAngularSize(actualDiameter, distance)
    }

    override fun getAugmentedRealityCoordinate(view: AugmentedRealityView): AugmentedRealityCoordinate {
        return AugmentedRealityCoordinate(
            AugmentedRealityUtils.toEastNorthUp(
                view.location,
                view.altitude,
                location,
                getActualElevation(view)
            ),
            true
        )
    }

    private fun getActualElevation(view: AugmentedRealityView): Float {
        return if (isElevationRelative){
            view.altitude + (elevation ?: 0f)
        } else {
            elevation ?: view.altitude
        }
    }
}