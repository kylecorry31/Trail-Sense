package com.kylecorry.trail_sense.tools.augmented_reality.guide

import com.kylecorry.trail_sense.tools.augmented_reality.AugmentedRealityView

interface ARGuide {
    fun start(arView: AugmentedRealityView)
    fun stop(arView: AugmentedRealityView)
}