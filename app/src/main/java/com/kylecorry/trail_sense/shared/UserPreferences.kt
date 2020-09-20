package com.kylecorry.trail_sense.shared

import android.content.Context
import android.hardware.SensorManager
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.infrastructure.AstronomyPreferences
import com.kylecorry.trail_sense.navigation.infrastructure.NavigationPreferences
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorChecker
import com.kylecorry.trail_sense.weather.infrastructure.WeatherPreferences
import com.kylecorry.trailsensecore.domain.units.PressureUnits
import com.kylecorry.trailsensecore.domain.units.TemperatureUnits

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

    val temperatureUnits: TemperatureUnits
        get() {
            return when (prefs.getString(getString(R.string.pref_temperature_units), "c")) {
                "f" -> TemperatureUnits.F
                else -> TemperatureUnits.C
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

    val experimentalEnabled: Boolean
        get() = prefs.getBoolean(getString(R.string.pref_enable_experimental), false)

    // Calibration

    var useAutoDeclination: Boolean
        get() = prefs.getBoolean(getString(R.string.pref_auto_declination), true)
        set(value) = prefs.edit { putBoolean(getString(R.string.pref_auto_declination), value) }

    var declinationOverride: Float
        get() = prefs.getString(getString(R.string.pref_declination_override), "0.0")
            ?.toFloatOrNull() ?: 0.0f
        set(value) = prefs.edit {
            putString(
                getString(R.string.pref_declination_override),
                value.toString()
            )
        }

    var useAutoLocation: Boolean
        get() = prefs.getBoolean(getString(R.string.pref_auto_location), true)
        set(value) = prefs.edit { putBoolean(getString(R.string.pref_auto_location), value) }

    var locationOverride: Coordinate
        get() {
            val latStr = prefs.getString(getString(R.string.pref_latitude_override), "0.0") ?: "0.0"
            val lngStr =
                prefs.getString(getString(R.string.pref_longitude_override), "0.0") ?: "0.0"

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
        get() = (prefs.getString(getString(R.string.pref_altitude_override), "0.0")
            ?: "0.0").toFloatOrNull() ?: 0.0f
        set(value) = prefs.edit {
            putString(
                getString(R.string.pref_altitude_override),
                value.toString()
            )
        }

    val altimeterMode: AltimeterMode
        get() {
            var raw = prefs.getString(getString(R.string.pref_altimeter_calibration_mode), null)

            if (raw == null){
                if (useAutoAltitude && useFineTuneAltitude && weather.hasBarometer){
                    raw = "gps_barometer"
                } else if (useAutoAltitude){
                    raw = "gps"
                }
            }

            return when (raw){
                "gps" -> AltimeterMode.GPS
                "gps_barometer" -> AltimeterMode.GPSBarometer
                "barometer" -> AltimeterMode.Barometer
                else -> AltimeterMode.Override
            }

        }

    var seaLevelPressureOverride: Float
        get() = prefs.getFloat(getString(R.string.pref_sea_level_pressure_override), SensorManager.PRESSURE_STANDARD_ATMOSPHERE)
        set(value) = prefs.edit { putFloat(getString(R.string.pref_sea_level_pressure_override), value)}

    private var useAutoAltitude: Boolean
        get() = prefs.getBoolean(getString(R.string.pref_auto_altitude), true)
        set(value) = prefs.edit { putBoolean(getString(R.string.pref_auto_altitude), value) }

    private var useFineTuneAltitude: Boolean
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

    enum class AltimeterMode {
        GPS,
        GPSBarometer,
        Barometer,
        Override
    }

}