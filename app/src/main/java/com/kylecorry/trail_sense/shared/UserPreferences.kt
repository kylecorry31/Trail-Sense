package com.kylecorry.trail_sense.shared

import android.content.Context
import android.hardware.SensorManager
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.infrastructure.AstronomyPreferences
import com.kylecorry.trail_sense.navigation.infrastructure.NavigationPreferences
import com.kylecorry.trail_sense.shared.math.MathExtensions.toFloatCompat2
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorChecker
import com.kylecorry.trail_sense.weather.infrastructure.WeatherPreferences
import com.kylecorry.trailsensecore.domain.units.PressureUnits
import com.kylecorry.trailsensecore.domain.units.TemperatureUnits
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import java.time.Duration

class UserPreferences(private val context: Context) {

    private val cache by lazy { Cache(context) }
    private val sensorChecker by lazy { SensorChecker(context) }

    val navigation by lazy { NavigationPreferences(context) }
    val weather by lazy { WeatherPreferences(context) }
    val astronomy by lazy { AstronomyPreferences(context) }

    val distanceUnits: DistanceUnits
        get() {
            val rawUnits =
                cache.getString(context.getString(R.string.pref_distance_units)) ?: "meters"
            return if (rawUnits == "meters") DistanceUnits.Meters else DistanceUnits.Feet
        }

    val pressureUnits: PressureUnits
        get() {
            return when (cache.getString(context.getString(R.string.pref_pressure_units))) {
                "in" -> PressureUnits.Inhg
                "mbar" -> PressureUnits.Mbar
                "psi" -> PressureUnits.Psi
                else -> PressureUnits.Hpa
            }
        }

    val temperatureUnits: TemperatureUnits
        get() {
            return when (cache.getString(getString(R.string.pref_temperature_units))) {
                "f" -> TemperatureUnits.F
                else -> TemperatureUnits.C
            }
        }

    val useLocationFeatures: Boolean
        get() = sensorChecker.hasGPS()

    val use24HourTime: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_use_24_hour)) ?: false

    val theme: Theme
        get() {
            if (isLowPowerModeOn) {
                return Theme.Black
            }

            return when (cache.getString(context.getString(R.string.pref_theme))) {
                "light" -> Theme.Light
                "dark" -> Theme.Dark
                "black" -> Theme.Black
                else -> Theme.System
            }
        }

    val experimentalEnabled: Boolean
        get() = cache.getBoolean(getString(R.string.pref_enable_experimental)) ?: false

    // Calibration

    var useAutoDeclination: Boolean
        get() = cache.getBoolean(getString(R.string.pref_auto_declination)) ?: true
        set(value) = cache.putBoolean(getString(R.string.pref_auto_declination), value)

    var declinationOverride: Float
        get() = (cache.getString(getString(R.string.pref_declination_override))
            ?: "0.0").toFloatCompat2() ?: 0.0f
        set(value) = cache.putString(
            getString(R.string.pref_declination_override),
            value.toString()
        )

    var useAutoLocation: Boolean
        get() = cache.getBoolean(getString(R.string.pref_auto_location)) ?: true
        set(value) = cache.putBoolean(getString(R.string.pref_auto_location), value)

    val requiresSatellites: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_require_satellites)) ?: true

    var locationOverride: Coordinate
        get() {
            val latStr = cache.getString(getString(R.string.pref_latitude_override)) ?: "0.0"
            val lngStr = cache.getString(getString(R.string.pref_longitude_override)) ?: "0.0"

            val lat = latStr.toDoubleOrNull() ?: 0.0
            val lng = lngStr.toDoubleOrNull() ?: 0.0

            return Coordinate(lat, lng)
        }
        set(value) {
            cache.putString(getString(R.string.pref_latitude_override), value.latitude.toString())
            cache.putString(getString(R.string.pref_longitude_override), value.longitude.toString())
        }

    var altitudeOverride: Float
        get(){
            val raw = cache.getString(getString(R.string.pref_altitude_override)) ?: "0.0"
            return raw.toFloatCompat2() ?: 0.0f
        }
        set(value) = cache.putString(getString(R.string.pref_altitude_override), value.toString())

    val altimeterMode: AltimeterMode
        get() {
            var raw = cache.getString(getString(R.string.pref_altimeter_calibration_mode))

            if (raw == null) {
                if (useAutoAltitude && useFineTuneAltitude && weather.hasBarometer) {
                    raw = "gps_barometer"
                } else if (useAutoAltitude) {
                    raw = "gps"
                }
            }

            return when (raw) {
                "gps" -> AltimeterMode.GPS
                "gps_barometer" -> AltimeterMode.GPSBarometer
                "barometer" -> AltimeterMode.Barometer
                else -> AltimeterMode.Override
            }

        }

    var seaLevelPressureOverride: Float
        get() = cache.getFloat(
            getString(R.string.pref_sea_level_pressure_override)
        ) ?: SensorManager.PRESSURE_STANDARD_ATMOSPHERE
        set(value) = cache.putFloat(
            getString(R.string.pref_sea_level_pressure_override),
            value
        )

    private var useAutoAltitude: Boolean
        get() = cache.getBoolean(getString(R.string.pref_auto_altitude)) ?: true
        set(value) = cache.putBoolean(getString(R.string.pref_auto_altitude), value)

    private var useFineTuneAltitude: Boolean
        get() = cache.getBoolean(getString(R.string.pref_fine_tune_altitude)) ?: true
        set(value) = cache.putBoolean(getString(R.string.pref_fine_tune_altitude), value)

    var useAltitudeOffsets: Boolean
        get() = cache.getBoolean(getString(R.string.pref_altitude_offsets)) ?: true
        set(value) = cache.putBoolean(getString(R.string.pref_altitude_offsets), value)

    var backtrackEnabled: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_backtrack_enabled)) ?: false
        set(value) = cache.putBoolean(
            context.getString(R.string.pref_backtrack_enabled),
            value
        )

    val backtrackRecordFrequency: Duration
        get() {
            val raw = cache.getString(getString(R.string.pref_backtrack_frequency)) ?: "30"
            return Duration.ofMinutes(raw.toLongOrNull() ?: 30L)
        }

    var isLowPowerModeOn: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_low_power_mode)) ?: false
        set(value) = cache.putBoolean(context.getString(R.string.pref_low_power_mode), value)

    val lowPowerModeDisablesWeather: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_low_power_mode_weather)) ?: true

    val lowPowerModeDisablesBacktrack: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_low_power_mode_backtrack)) ?: true

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