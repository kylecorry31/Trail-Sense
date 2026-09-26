package com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.GeographicARPoint
import com.kylecorry.trail_sense.tools.navigation.domain.Destination

data class PathGuidanceTarget(val destination: Destination.Path) : ARGuidanceTarget {
    override suspend fun refresh(request: ARGuidanceRefreshRequest): ARGuidanceTargetState {
        return ARGuidanceTargetState(
            ARGuidanceDisplayState(
                destination.path.name ?: request.context.getString(R.string.path),
                R.drawable.path_arrow,
                iconTint = destination.path.style.color
            ),
            GeographicARPoint(destination.route.navigate(request.location).target)
        )
    }
}
