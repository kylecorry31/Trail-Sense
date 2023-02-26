package com.kylecorry.trail_sense.shared.extensions

import com.kylecorry.andromeda.preferences.IPreferences
import java.time.Duration

fun IPreferences.putDuration(key: String, duration: Duration) {
    putLong(key, duration.toMillis())
}

fun IPreferences.getDuration(key: String): Duration? {
    val millis = getLong(key) ?: return null
    return Duration.ofMillis(millis)
}