package com.kylecorry.trail_sense.tools.augmented_reality.position

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.camera.AugmentedRealityUtils
import com.kylecorry.trail_sense.tools.augmented_reality.AugmentedRealityView
import kotlin.math.hypot

class GeographicPositionStrategy(
    private val location: Coordinate,
    private val elevation: Float? = null,
    private val actualDiameter: Float = 1f
) : ARPositionStrategy {
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