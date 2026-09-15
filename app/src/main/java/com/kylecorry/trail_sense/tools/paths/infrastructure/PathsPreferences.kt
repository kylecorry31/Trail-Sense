package com.kylecorry.trail_sense.tools.paths.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.StringEnumPreference
import com.kylecorry.andromeda.sense.location.GPSPowerUsage
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.infrastructure.GPSPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.gps.GPSPowerMode
import java.time.Duration

class PathsPreferences(private val context: Context) {

    private val cache by lazy { PreferencesSubsystem.getInstance(context).preferences }
    private val gps by lazy { GPSPreferences(context) }

    var backtrackEnabled by BooleanPreference(
        cache,
        context.getString(R.string.pref_backtrack_enabled),
        false
    )

    var backtrackSaveCellHistory by BooleanPreference(
        cache,
        context.getString(R.string.pref_backtrack_save_cell),
        true
    )

    val backtrackKeepDeviceAwake by BooleanPreference(
        cache,
        context.getString(R.string.pref_backtrack_keep_awake),
        false
    )

    private val backtrackGPSPowerMode by StringEnumPreference(
        cache,
        context.getString(R.string.pref_backtrack_gps_power_usage),
        GPSPowerMode.entries.associateBy { it.id.toString() },
        GPSPowerMode.Inherit
    )

    val backtrackGPSPowerUsage: GPSPowerUsage
        get() = backtrackGPSPowerMode.powerUsage ?: gps.powerMode.powerUsage ?: GPSPowerUsage.High

    var backtrackRecordFrequency: Duration
        get() = cache.getDuration(context.getString(R.string.pref_backtrack_frequency))
            ?: Duration.ofMinutes(1)
        set(value) {
            cache.putDuration(context.getString(R.string.pref_backtrack_frequency), value)
        }
}
