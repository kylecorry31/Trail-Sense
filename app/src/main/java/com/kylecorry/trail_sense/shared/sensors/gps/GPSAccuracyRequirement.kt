package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.trail_sense.shared.data.Identifiable

enum class GPSAccuracyRequirement(
    override val id: Long,
    val minAccuracy: Float?,
    val maxRejectionCount: Int?
) : Identifiable {
    Low(1, null, null),
    Medium(2, 16f, 3),
    High(3, 8f, 4)
}
