package com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.GeographicARPoint
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.SphericalARPoint
import com.kylecorry.trail_sense.tools.navigation.domain.Destination

data class BearingGuidanceTarget(
    val destination: Destination.Bearing,
    val lockBearingToLocation: Boolean
) : ARGuidanceTarget {
    override suspend fun refresh(request: ARGuidanceRefreshRequest): ARGuidanceTargetState {
        val targetLocation = destination.targetLocation
        val point = if (lockBearingToLocation && targetLocation != null) {
            GeographicARPoint(targetLocation)
        } else {
            SphericalARPoint(
                destination.bearing.value,
                0f,
                isTrueNorth = destination.isTrueNorth
            )
        }
        return ARGuidanceTargetState(
            ARGuidanceDisplayState(
                request.context.getString(R.string.bearing),
                R.drawable.ic_compass_icon
            ),
            point
        )
    }
}
