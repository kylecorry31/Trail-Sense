package com.kylecorry.trail_sense.tools.augmented_reality.domain.position

import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView

interface ARPoint {

    fun getAugmentedRealityCoordinate(view: AugmentedRealityView): AugmentedRealityCoordinate

    /**
     * Gets the angular diameter of the point in degrees
     */
    fun getAngularDiameter(view: AugmentedRealityView): Float
}
