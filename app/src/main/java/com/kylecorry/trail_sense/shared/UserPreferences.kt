package com.kylecorry.trail_sense.shared

import android.content.Context
import android.hardware.SensorManager
import android.text.format.DateFormat
import com.kylecorry.andromeda.core.toFloatCompat
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.preferences.StringEnumPreference
import com.kylecorry.sol.units.*
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.infrastructure.AstronomyPreferences
import com.kylecorry.trail_sense.navigation.infrastructure.NavigationPreferences
import com.kylecorry.trail_sense.settings.infrastructure.*
import com.kylecorry.trail_sense.shared.sharing.MapSite
import com.kylecorry.trail_sense.weather.infrastructure.WeatherPreferences
import java.time.Duration

class UserPreferences(private val context: Context): IDeclinationPreferences {

    private val cache by lazy { Preferences(context) }

    val navigation by lazy { NavigationPreferences(context) }
    val weather by lazy { WeatherPreferences(context) }
    val astronomy by lazy { AstronomyPreferences(context) }
    val flashlight by lazy { FlashlightPreferenceRepo(context) }
    val cellSignal by lazy { CellSignalPreferences(context) }
    val metalDetector by lazy { MetalDetectorPreferences(context) }
    val privacy by lazy { PrivacyPreferences(context) }
    val tides by lazy { TidePreferences(context) }
    val power by lazy { PowerPreferences(context) }
    val packs by lazy { PackPreferences(context) }
    val depth by lazy { DepthPreferences(context) }

    val distanceUnits: DistanceUnits
        get() {
            val rawUnits =
                cache.getString(context.getString(R.string.pref_distance_units)) ?: "meters"
            return if (rawUnits == "meters") DistanceUnits.Meters else DistanceUnits.Feet
        }

    val weightUnits by StringEnumPreference(
        cache, getString(R.string.pref_weight_units), mapOf(
            "kg" to WeightUnits.Kilograms,
            "lbs" to WeightUnits.Pounds
        ),
        WeightUnits.Kilograms
    )

    val baseDistanceUnits: com.kylecorry.sol.units.DistanceUnits
        get() = if (distanceUnits == DistanceUnits.Meters) com.kylecorry.sol.units.DistanceUnits.Meters else com.kylecorry.sol.units.DistanceUnits.Feet

    val pressureUnits: PressureUnits
        get() {
            return when (cache.getString(context.getString(R.string.pref_pressure_units))) {
                "in" -> PressureUnits.Inhg
                "mbar" -> PressureUnits.Mbar
                "psi" -> PressureUnits.Psi
                "mm" -> PressureUnits.MmHg
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

    val use24HourTime: Boolean
        get() {
            val value = cache.getBoolean(context.getString(R.string.pref_use_24_hour))
            return if (value == null) {
                val system = DateFormat.is24HourFormat(context)
                cache.putBoolean(context.getString(R.string.pref_use_24_hour), system)
                system
            } else {
                value
            }

        }

    val theme: Theme
        get() {
            if (isLowPowerModeOn) {
                return Theme.Black
            }

            return when (cache.getString(context.getString(R.string.pref_theme))) {
                "light" -> Theme.Light
                "dark" -> Theme.Dark
                "black" -> Theme.Black
                "sunrise_sunset" -> Theme.SunriseSunset
                else -> Theme.System
            }
        }

    // Calibration

    override var useAutoDeclination: Boolean
        get() = cache.getBoolean(getString(R.string.pref_auto_declination)) ?: true
        set(value) = cache.putBoolean(getString(R.string.pref_auto_declination), value)

    override var declinationOverride: Float
        get() = (cache.getString(getString(R.string.pref_declination_override))
            ?: "0.0").toFloatCompat() ?: 0.0f
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
        get() {
            val raw = cache.getString(getString(R.string.pref_altitude_override)) ?: "0.0"
            return raw.toFloatCompat() ?: 0.0f
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

    val useNMEA: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_nmea_altitude)) ?: false

    val odometerDistanceThreshold: Distance
        get() = Distance.meters(15f)

    var backtrackEnabled: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_backtrack_enabled)) ?: false
        set(value) = cache.putBoolean(
            context.getString(R.string.pref_backtrack_enabled),
            value
        )

    var backtrackSaveCellHistory by BooleanPreference(
        cache,
        context.getString(R.string.pref_backtrack_save_cell),
        true
    )

    var backtrackRecordFrequency: Duration
        get() {
            val raw = cache.getString(getString(R.string.pref_backtrack_frequency)) ?: "30"
            return Duration.ofMinutes(raw.toLongOrNull() ?: 30L)
        }
        set(value) {
            cache.putString(
                getString(R.string.pref_backtrack_frequency),
                value.toMinutes().toString()
            )
        }

    var isLowPowerModeOn: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_low_power_mode)) ?: false
        set(value) = cache.putBoolean(context.getString(R.string.pref_low_power_mode), value)

    val lowPowerModeDisablesWeather: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_low_power_mode_weather)) ?: true

    val lowPowerModeDisablesBacktrack: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_low_power_mode_backtrack)) ?: true

    var lastTide: Long?
        get() = cache.getLong(context.getString(R.string.last_tide_id))
        set(value) {
            if (value != null) {
                cache.putLong(context.getString(R.string.last_tide_id), value)
            } else {
                cache.remove(context.getString(R.string.last_tide_id))
            }
        }

    var usePedometer: Boolean
        get() {
            val raw = cache.getString(getString(R.string.pref_odometer_source))
            return raw == "pedometer"
        }
        set(value) {
            val str = if (value) {
                "pedometer"
            } else {
                "gps"
            }
            cache.putString(getString(R.string.pref_odometer_source), str)
        }

    val resetOdometerDaily: Boolean
        get() {
            return cache.getBoolean(getString(R.string.pref_odometer_reset_daily)) ?: false
        }

    var strideLength: Distance
        get() {
            val raw = cache.getFloat(getString(R.string.pref_stride_length)) ?: 0.7f
            return Distance.meters(raw)
        }
        set(value) {
            cache.putFloat(getString(R.string.pref_stride_length), value.meters().distance)
        }

    val mapSite: MapSite by StringEnumPreference(
        cache, context.getString(R.string.pref_map_url_source), mapOf(
            "apple" to MapSite.Apple,
            "bing" to MapSite.Bing,
            "caltopo" to MapSite.Caltopo,
            "google" to MapSite.Google,
            "osm" to MapSite.OSM
        ), MapSite.OSM
    )

    private fun getString(id: Int): String {
        return context.getString(id)
    }

    enum class DistanceUnits {
        Meters, Feet
    }

    enum class Theme {
        Light, Dark, Black, System, SunriseSunset
    }

    enum class AltimeterMode {
        GPS,
        GPSBarometer,
        Barometer,
        Override
    }

}