package com.kylecorry.trail_sense.tools.augmented_reality.guide

import android.widget.FrameLayout
import com.kylecorry.trail_sense.tools.augmented_reality.AugmentedRealityView

interface ARGuide {
    fun start(arView: AugmentedRealityView, panel: FrameLayout)
    fun stop(arView: AugmentedRealityView, panel: FrameLayout)
}