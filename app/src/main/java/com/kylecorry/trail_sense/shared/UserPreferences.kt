package com.kylecorry.trail_sense.shared

import android.content.Context
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.infrastructure.AstronomyPreferences
import com.kylecorry.trail_sense.navigation.infrastructure.NavigationPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorChecker
import com.kylecorry.trail_sense.weather.domain.PressureUnits
import com.kylecorry.trail_sense.weather.infrastructure.WeatherPreferences

class UserPreferences(private val context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val sensorChecker = SensorChecker(context)

    val navigation = NavigationPreferences(context)
    val weather = WeatherPreferences(context)
    val astronomy = AstronomyPreferences(context)

    val distanceUnits: DistanceUnits
        get() {
            val rawUnits =
                prefs.getString(context.getString(R.string.pref_distance_units), "meters")
                    ?: "meters"
            return if (rawUnits == "meters") DistanceUnits.Meters else DistanceUnits.Feet
        }

    val pressureUnits: PressureUnits
        get() {
            return when (prefs.getString(context.getString(R.string.pref_pressure_units), "hpa")) {
                "hpa" -> PressureUnits.Hpa
                "in" -> PressureUnits.Inhg
                "mbar" -> PressureUnits.Mbar
                "psi" -> PressureUnits.Psi
                else -> PressureUnits.Hpa
            }
        }

    val useLocationFeatures: Boolean
        get() = sensorChecker.hasGPS()

    val use24HourTime: Boolean
        get() = prefs.getBoolean(context.getString(R.string.pref_use_24_hour), false)

    val theme: Theme
        get() {
            return when (prefs.getString(context.getString(R.string.pref_theme), "system")) {
                "light" -> Theme.Light
                "dark" -> Theme.Dark
                else -> Theme.System
            }
        }

    enum class DistanceUnits {
        Meters, Feet
    }

    enum class Theme {
        Light, Dark, System
    }

}