package com.kylecorry.trail_sense.settings.migrations

import android.content.Context
import com.kylecorry.andromeda.core.system.Screen
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounter
import java.time.Duration

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

        private const val version = 9
        private val migrations = listOf(
            PreferenceMigration(0, 1) { context, prefs ->
                if (prefs.contains("pref_enable_experimental")) {
                    val isExperimental = prefs.getBoolean("pref_enable_experimental") ?: false
                    prefs.putBoolean(
                        context.getString(R.string.pref_experimental_maps),
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
            },
            PreferenceMigration(5, 6) { _, prefs ->
                prefs.remove("pref_experimental_barometer_calibration")
                prefs.remove("pref_sea_level_require_dwell")
                prefs.remove("pref_barometer_altitude_change")
                prefs.remove("pref_sea_level_pressure_change_thresh")
                prefs.remove("pref_sea_level_use_rapid")
            },
            PreferenceMigration(6, 7) { context, prefs ->
                val distance = prefs.getFloat("odometer_distance")
                if (distance != null) {
                    val stride = UserPreferences(context).pedometer.strideLength.meters().distance
                    if (stride > 0f) {
                        val steps = (distance / stride).toLong()
                        prefs.putLong(StepCounter.STEPS_KEY, steps)
                    }
                }
                prefs.remove("odometer_distance")
                prefs.remove("last_odometer_location")

                prefs.putBoolean(
                    context.getString(R.string.pref_pedometer_enabled),
                    prefs.getString("pref_odometer_source") == "pedometer"
                )
            },
            PreferenceMigration(7, 8) { context, _ ->
                val prefs = UserPreferences(context).navigation
                val currentScale = prefs.rulerScale
                if (currentScale == 1f || currentScale == 0f) {
                    return@PreferenceMigration
                }

                val dpi = Screen.dpi(context)
                val ydpi = Screen.ydpi(context)
                val adjustedDpi = dpi / currentScale
                prefs.rulerScale = ydpi / adjustedDpi
            },
            PreferenceMigration(8, 9) { context, prefs ->
                val userPrefs = UserPreferences(context)
                prefs.getString("pref_backtrack_frequency")?.toLongOrNull()?.let {
                    userPrefs.backtrackRecordFrequency = Duration.ofMinutes(it)
                }
                prefs.getString("pref_weather_update_frequency")?.toLongOrNull()?.let {
                    userPrefs.weather.weatherUpdateFrequency = Duration.ofMinutes(it)
                }
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