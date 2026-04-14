package com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance

import com.kylecorry.trail_sense.tools.beacons.domain.Beacon

interface ARBeaconGuidanceTarget : ARGuidanceTarget {
    val beacon: Beacon
}
