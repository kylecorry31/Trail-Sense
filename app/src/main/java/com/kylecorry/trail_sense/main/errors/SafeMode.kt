package com.kylecorry.trail_sense.main.errors

import android.content.Context
import android.util.Log
import com.kylecorry.trail_sense.shared.debugging.isDebug
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem

object SafeMode {

    fun initialize(context: Context) {
        val preferences = PreferencesSubsystem.getInstance(context).preferences

        val lastStartTime = preferences.getLong("cache_last_start_time") ?: 0L
        val lastMinus1StartTime = preferences.getLong("cache_last_minus_1_start_time") ?: 0L
        preferences.putLong(
            "cache_last_start_time",
            System.currentTimeMillis()
        )

        preferences.putLong(
            "cache_last_minus_1_start_time",
            lastStartTime
        )

        if ((System.currentTimeMillis() - lastStartTime) <= RESTART_CRASH_THRESHOLD_MILLIS && (lastStartTime - lastMinus1StartTime) <= RESTART_CRASH_THRESHOLD_MILLIS) {
            // If the app was restarted within the threshold, we are in safe mode
            Log.w("TrailSenseApplication", "App restarted within threshold, entering safe mode")
            safeModeEndTime = System.currentTimeMillis() + SAFE_MODE_DURATION_MILLIS
        } else {
            safeModeEndTime = 0L
        }
    }

    private val RESTART_CRASH_THRESHOLD_MILLIS = 30 * 1000 // 30 seconds
    private val SAFE_MODE_DURATION_MILLIS = 20 * 1000 // 20 seconds
    private var safeModeEndTime: Long = 0L

    fun isEnabled(): Boolean {
        return !isDebug() && safeModeEndTime > 0 && System.currentTimeMillis() < safeModeEndTime
    }

}