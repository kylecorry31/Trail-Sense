package com.kylecorry.trail_sense.tools.augmented_reality.domain.position

import com.kylecorry.trail_sense.shared.camera.AugmentedRealityUtils
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView

/**
 * A point in spherical coordinates
 * @param bearing The bearing in degrees
 * @param elevationAngle The elevation angle in degrees
 * @param distance The distance in meters, defaults to MAX_VALUE
 * @param angularDiameter The angular diameter in degrees, defaults to 1
 * @param isTrueNorth True if the bearing is true north, false if it is magnetic north
 */
class SphericalARPoint(
    bearing: Float,
    elevationAngle: Float,
    distance: Float = Float.MAX_VALUE,
    private val angularDiameter: Float = 1f,
    isTrueNorth: Boolean = true
) : ARPoint {
    val coordinate = AugmentedRealityCoordinate(
        AugmentedRealityUtils.toEastNorthUp(
            bearing,
            elevationAngle,
            distance
        ), isTrueNorth
    )

    override fun getAngularDiameter(view: AugmentedRealityView): Float {
        return angularDiameter
    }

    override fun getAugmentedRealityCoordinate(view: AugmentedRealityView): AugmentedRealityCoordinate {
        return coordinate
    }
}