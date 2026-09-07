package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.trail_sense.shared.data.Identifiable
import java.time.Duration

enum class GPSAccuracyFilter(
    override val id: Long,
    val minAccuracy: Float?,
    val maxAccuracyWait: Duration?
) : Identifiable {
    None(1, null, null),
    Low(2, 30f, Duration.ofSeconds(3)),
    // Corresponds to "Moderate" quality
    Moderate(3, 16f, Duration.ofSeconds(8)),
    // Corresponds to "Good" quality
    High(4, 8f, Duration.ofSeconds(12))
}
