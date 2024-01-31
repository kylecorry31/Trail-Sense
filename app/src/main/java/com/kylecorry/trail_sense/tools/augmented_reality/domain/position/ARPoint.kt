package com.kylecorry.trail_sense.tools.augmented_reality.domain.position

import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView

/**
 * A point in the AR world
 */
interface ARPoint {

    /**
     * Gets the AR coordinate of the point
     */
    fun getAugmentedRealityCoordinate(view: AugmentedRealityView): AugmentedRealityCoordinate

    /**
     * Gets the angular diameter of the point in degrees
     */
    fun getAngularDiameter(view: AugmentedRealityView): Float
}