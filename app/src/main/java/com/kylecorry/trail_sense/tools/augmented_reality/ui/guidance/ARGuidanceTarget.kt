package com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance

import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView

interface ARGuidanceTarget {
    suspend fun refresh(view: AugmentedRealityView): ARGuidanceTargetState
}
