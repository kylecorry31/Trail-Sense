package com.kylecorry.trail_sense.shared

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.infrastructure.AstronomyPreferences
import com.kylecorry.trail_sense.navigation.infrastructure.NavigationPreferences
import com.kylecorry.trail_sense.shared.domain.Coordinate
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
                "black" -> Theme.Black
                else -> Theme.System
            }
        }

    // Calibration

    var useAutoDeclination: Boolean
        get() = prefs.getBoolean(getString(R.string.pref_auto_declination), true)
        set(value) = prefs.edit { putBoolean(getString(R.string.pref_auto_declination), value) }

    var declinationOverride: Float
        get() = prefs.getString(getString(R.string.pref_declination_override), "0.0")?.toFloatOrNull() ?: 0.0f
        set(value) = prefs.edit { putString(getString(R.string.pref_declination_override), value.toString()) }

    var useAutoLocation: Boolean
        get() = prefs.getBoolean(getString(R.string.pref_auto_location), true)
        set(value) = prefs.edit { putBoolean(getString(R.string.pref_auto_location), value) }

    var locationOverride: Coordinate
        get() {
            val latStr = prefs.getString(getString(R.string.pref_latitude_override), "0.0") ?: "0.0"
            val lngStr = prefs.getString(getString(R.string.pref_longitude_override), "0.0") ?: "0.0"

            val lat = latStr.toDoubleOrNull() ?: 0.0
            val lng = lngStr.toDoubleOrNull() ?: 0.0

            return Coordinate(lat, lng)
        }
        set(value) {
            prefs.edit {
                putString(getString(R.string.pref_latitude_override), value.latitude.toString())
                putString(getString(R.string.pref_longitude_override), value.longitude.toString())
            }
        }

    var altitudeOverride: Float
        get() = (prefs.getString(getString(R.string.pref_altitude_override), "0.0") ?: "0.0").toFloatOrNull() ?: 0.0f
        set(value) = prefs.edit { putString(getString(R.string.pref_altitude_override), value.toString()) }

    var useAutoAltitude: Boolean
        get() = prefs.getBoolean(getString(R.string.pref_auto_altitude), true)
        set(value) = prefs.edit { putBoolean(getString(R.string.pref_auto_altitude), value) }

    var useFineTuneAltitude: Boolean
        get() = prefs.getBoolean(getString(R.string.pref_fine_tune_altitude), true)
        set(value) = prefs.edit { putBoolean(getString(R.string.pref_fine_tune_altitude), value) }

    var useAltitudeOffsets: Boolean
        get() = prefs.getBoolean(getString(R.string.pref_altitude_offsets), true)
        set(value) = prefs.edit { putBoolean(getString(R.string.pref_altitude_offsets), value) }

    private fun getString(id: Int): String {
        return context.getString(id)
    }

    enum class DistanceUnits {
        Meters, Feet
    }

    enum class Theme {
        Light, Dark, Black, System
    }

}