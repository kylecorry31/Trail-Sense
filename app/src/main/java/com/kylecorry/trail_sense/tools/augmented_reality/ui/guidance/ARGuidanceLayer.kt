package com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance

import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView

interface ARGuidanceLayer {
    val guidanceName: String
    suspend fun pickGuidanceTarget(view: AugmentedRealityView): ARGuidanceTarget?
}
