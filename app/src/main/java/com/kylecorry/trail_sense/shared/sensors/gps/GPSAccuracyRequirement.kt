package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.trail_sense.shared.data.Identifiable
import java.time.Duration

enum class GPSAccuracyRequirement(
    override val id: Long,
    val minAccuracy: Float?,
    val maxAccuracyWait: Duration?
) : Identifiable {
    Low(1, null, null),
    Medium(2, 16f, Duration.ofSeconds(5)),
    High(3, 8f, Duration.ofSeconds(10))
}
