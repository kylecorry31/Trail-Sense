package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.andromeda.core.time.TimeProvider
import com.kylecorry.andromeda.sense.location.IGPS
import java.time.Duration

fun IGPS.durationSince(elapsedNanos: Long): Duration {
    return Duration.ofNanos(eventTimeElapsedNanos - elapsedNanos)
}

fun IGPS.durationSince(other: IGPS): Duration {
    return durationSince(other.eventTimeElapsedNanos)
}

fun IGPS.age(timeProvider: TimeProvider): Duration {
    return Duration.ofMillis(timeProvider.elapsedRealtime()).minusNanos(eventTimeElapsedNanos)
}
