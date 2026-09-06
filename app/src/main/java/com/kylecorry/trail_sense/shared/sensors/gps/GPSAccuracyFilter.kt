package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.trail_sense.shared.data.Identifiable
import java.time.Duration

enum class GPSAccuracyFilter(
    override val id: Long,
    val minAccuracy: Float?,
    val maxAccuracyWait: Duration?
) : Identifiable {
    None(1, null, null),
    Moderate(2, 16f, Duration.ofSeconds(5)),
    High(3, 8f, Duration.ofSeconds(10))
}
