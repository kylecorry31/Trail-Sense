package com.kylecorry.trail_sense.tools.augmented_reality.position

import com.kylecorry.trail_sense.tools.augmented_reality.AugmentedRealityView

class SphericalPositionStrategy(
    bearing: Float,
    elevation: Float,
    distance: Float = Float.MAX_VALUE,
    private val angularDiameter: Float = 1f,
    isTrueNorth: Boolean = true
) : ARPositionStrategy {
    private val position =
        AugmentedRealityView.HorizonCoordinate(bearing, elevation, distance, isTrueNorth)

    override fun getAngularDiameter(view: AugmentedRealityView): Float {
        return angularDiameter
    }

    override fun getHorizonCoordinate(view: AugmentedRealityView): AugmentedRealityView.HorizonCoordinate {
        return position
    }
}