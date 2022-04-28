package com.kylecorry.trail_sense.shared.extensions

import com.kylecorry.andromeda.preferences.Preferences
import java.time.Duration

fun Preferences.putDuration(key: String, duration: Duration) {
    putLong(key, duration.toMillis())
}

fun Preferences.getDuration(key: String): Duration? {
    val millis = getLong(key) ?: return null
    return Duration.ofMillis(millis)
}