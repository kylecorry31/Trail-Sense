package com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance

import android.graphics.Color
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.GeographicARPoint
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon

data class BeaconGuidanceTarget(
    override val beacon: Beacon
) : ARBeaconGuidanceTarget {
    override suspend fun refresh(view: AugmentedRealityView): ARGuidanceTargetState {
        return ARGuidanceTargetState(
            ARGuidanceDisplayState(
                beacon.name,
                beacon.icon?.icon ?: R.drawable.ic_location,
                iconTint = Colors.mostContrastingColor(Color.WHITE, Color.BLACK, beacon.color),
                iconBackgroundTint = beacon.color
            ),
            GeographicARPoint(beacon.coordinate, beacon.elevation)
        )
    }
}
