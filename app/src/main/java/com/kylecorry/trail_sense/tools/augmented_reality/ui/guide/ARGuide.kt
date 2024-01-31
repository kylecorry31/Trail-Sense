package com.kylecorry.trail_sense.tools.augmented_reality.ui.guide

import android.widget.FrameLayout
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView

interface ARGuide {
    fun start(arView: AugmentedRealityView, panel: FrameLayout)
    fun stop(arView: AugmentedRealityView, panel: FrameLayout)
}