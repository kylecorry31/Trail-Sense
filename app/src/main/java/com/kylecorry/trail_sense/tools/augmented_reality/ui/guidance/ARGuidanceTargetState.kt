package com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance

import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.ARPoint

data class ARGuidanceTargetState(
    val display: ARGuidanceDisplayState,
    val point: ARPoint
)
