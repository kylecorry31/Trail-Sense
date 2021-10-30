package com.kylecorry.trail_sense.settings.migrations

import android.content.Context
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.R

class PreferenceMigrator private constructor() {

    private val lock = Object()

    fun migrate(context: Context) {
        synchronized(lock) {
            val prefs = Preferences(context)
            var currentVersion = prefs.getInt("pref_version") ?: 0

            while (currentVersion < version) {
                val current = currentVersion
                val next = currentVersion + 1
                val migration =
                    migrations.find { it.fromVersion == current && it.toVersion == next }
                migration?.action?.invoke(context, prefs)
                currentVersion++
                prefs.putInt("pref_version", currentVersion)
            }
        }
    }

    companion object {
        private var instance: PreferenceMigrator? = null
        private val staticLock = Object()

        private const val version = 5
        private val migrations = listOf(
            PreferenceMigration(0, 1) { context, prefs ->
                if (prefs.contains("pref_enable_experimental")) {
                    val isExperimental = prefs.getBoolean("pref_enable_experimental") ?: false
                    prefs.putBoolean(
                        context.getString(R.string.pref_experimental_maps),
                        isExperimental
                    )
                    prefs.putBoolean(
                        context.getString(R.string.pref_experimental_tide_clock),
                        isExperimental
                    )
                    prefs.remove("pref_enable_experimental")
                    prefs.remove("pref_use_camera_features")
                }
            },
            PreferenceMigration(1, 2) { context, prefs ->
                if (prefs.getBoolean(context.getString(R.string.pref_onboarding_completed)) == true) {
                    if (!prefs.contains(context.getString(R.string.pref_sunset_alerts))) {
                        prefs.putBoolean(context.getString(R.string.pref_sunset_alerts), true)
                    }

                    if (!prefs.contains(context.getString(R.string.pref_monitor_weather))) {
                        prefs.putBoolean(context.getString(R.string.pref_monitor_weather), true)
                    }
                }
            },
            PreferenceMigration(2, 3) { _, prefs ->
                prefs.remove("cache_pressure_setpoint")
                prefs.remove("cache_pressure_setpoint_altitude")
                prefs.remove("cache_pressure_setpoint_temperature")
                prefs.remove("cache_pressure_setpoint_time")
            },
            PreferenceMigration(3, 4) { context, prefs ->
                try {
                    val color = prefs.getInt(context.getString(R.string.pref_backtrack_path_color))
                        ?: return@PreferenceMigration
                    prefs.remove(context.getString(R.string.pref_backtrack_path_color))
                    prefs.putLong(
                        context.getString(R.string.pref_backtrack_path_color),
                        color.toLong()
                    )
                } catch (e: Exception) {
                    prefs.remove(context.getString(R.string.pref_backtrack_path_color))
                }
            },
            PreferenceMigration(4, 5) { _, prefs ->
                prefs.remove("pref_path_waypoint_style")
            }
        )

        fun getInstance(): PreferenceMigrator {
            return synchronized(staticLock) {
                if (instance == null) {
                    instance = PreferenceMigrator()
                }
                instance!!
            }
        }


    }

}