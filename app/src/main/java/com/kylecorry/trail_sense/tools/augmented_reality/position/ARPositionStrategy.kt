package com.kylecorry.trail_sense.tools.augmented_reality.position

import com.kylecorry.trail_sense.tools.augmented_reality.AugmentedRealityView

interface ARPositionStrategy {
    fun getHorizonCoordinate(view: AugmentedRealityView): AugmentedRealityView.HorizonCoordinate
    fun getAngularDiameter(view: AugmentedRealityView): Float
}