package com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance

interface ARGuidanceTarget {
    suspend fun refresh(request: ARGuidanceRefreshRequest): ARGuidanceTargetState
}
