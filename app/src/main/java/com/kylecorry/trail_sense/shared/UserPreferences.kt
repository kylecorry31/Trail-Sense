package com.kylecorry.trail_sense.shared

import android.content.Context
import android.hardware.SensorManager
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.toFloatCompat
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.IntEnumPreference
import com.kylecorry.andromeda.preferences.IntPreference
import com.kylecorry.andromeda.preferences.StringEnumPreference
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.sol.units.WeightUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.CustomBottomNavigationView
import com.kylecorry.trail_sense.settings.backup.BackupPreferences
import com.kylecorry.trail_sense.settings.infrastructure.AltimeterPreferences
import com.kylecorry.trail_sense.settings.infrastructure.AugmentedRealityPreferences
import com.kylecorry.trail_sense.settings.infrastructure.CameraPreferences
import com.kylecorry.trail_sense.settings.infrastructure.CellSignalPreferences
import com.kylecorry.trail_sense.settings.infrastructure.ClinometerPreferences
import com.kylecorry.trail_sense.settings.infrastructure.ClockPreferences
import com.kylecorry.trail_sense.settings.infrastructure.CompassPreferences
import com.kylecorry.trail_sense.settings.infrastructure.ErrorPreferences
import com.kylecorry.trail_sense.settings.infrastructure.FlashlightPreferenceRepo
import com.kylecorry.trail_sense.settings.infrastructure.IDeclinationPreferences
import com.kylecorry.trail_sense.settings.infrastructure.MetalDetectorPreferences
import com.kylecorry.trail_sense.settings.infrastructure.PackPreferences
import com.kylecorry.trail_sense.settings.infrastructure.PedometerPreferences
import com.kylecorry.trail_sense.settings.infrastructure.PowerPreferences
import com.kylecorry.trail_sense.settings.infrastructure.PrivacyPreferences
import com.kylecorry.trail_sense.settings.infrastructure.ThermometerPreferences
import com.kylecorry.trail_sense.settings.infrastructure.TidePreferences
import com.kylecorry.trail_sense.shared.alerts.NotificationSubsystem
import com.kylecorry.trail_sense.shared.extensions.getIntArray
import com.kylecorry.trail_sense.shared.extensions.getLongArray
import com.kylecorry.trail_sense.shared.extensions.putIntArray
import com.kylecorry.trail_sense.shared.extensions.putLongArray
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sharing.MapSite
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.AstronomyPreferences
import com.kylecorry.trail_sense.tools.ballistics.infrastructure.BallisticsPreferences
import com.kylecorry.trail_sense.tools.climate.infrastructure.ClimatePreferenceRepo
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuidePreferences
import com.kylecorry.trail_sense.tools.map.infrastructure.MapPreferences
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.PhotoMapPreferences
import com.kylecorry.trail_sense.tools.navigation.infrastructure.NavigationPreferences
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.sort.ToolSortType
import com.kylecorry.trail_sense.tools.turn_back.infrastructure.TurnBackPreferences
import com.kylecorry.trail_sense.tools.waterpurification.infrastructure.WaterBoilTimerPreferences
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherPreferences
import java.time.Duration

class UserPreferences(ctx: Context) : IDeclinationPreferences {

    private val context = ctx.applicationContext
    private val cache by lazy { PreferencesSubsystem.getInstance(context).preferences }

    val navigation by lazy { NavigationPreferences(context) }
    val photoMaps by lazy { PhotoMapPreferences(context) }
    val map by lazy { MapPreferences(context) }
    val weather by lazy { WeatherPreferences(context) }
    val astronomy by lazy { AstronomyPreferences(context) }
    val flashlight by lazy { FlashlightPreferenceRepo(context) }
    val cellSignal by lazy { CellSignalPreferences(context) }
    val metalDetector by lazy { MetalDetectorPreferences(context) }
    val privacy by lazy { PrivacyPreferences(context) }
    val tides by lazy { TidePreferences(context) }
    val power by lazy { PowerPreferences(context) }
    val packs by lazy { PackPreferences(context) }
    val clinometer by lazy { ClinometerPreferences(context) }
    val errors by lazy { ErrorPreferences(context) }
    val pedometer by lazy { PedometerPreferences(context) }
    val thermometer by lazy { ThermometerPreferences(context) }
    val compass by lazy { CompassPreferences(context) }
    val clock by lazy { ClockPreferences(context) }
    val camera by lazy { CameraPreferences(context) }
    val altimeter by lazy { AltimeterPreferences(context) }
    val augmentedReality by lazy { AugmentedRealityPreferences(context) }
    val backup by lazy { BackupPreferences(context) }
    val ballistics by lazy { BallisticsPreferences(context) }
    val climate by lazy { ClimatePreferenceRepo(context) }
    val fieldGuide by lazy { FieldGuidePreferences(context) }
    val waterBoilTimer by lazy { WaterBoilTimerPreferences(context) }
    val turnBack by lazy { TurnBackPreferences(context) }

    private val isMetricPreferred = Resources.isMetricPreferred(context)

    val useFilteredGPS by BooleanPreference(
        cache,
        context.getString(R.string.pref_use_filtered_gps),
        false
    )

    var isCliffHeightEnabled by BooleanPreference(
        cache,
        context.getString(R.string.pref_cliff_height_enabled),
        false
    )

    val distanceUnits by StringEnumPreference(
        cache,
        getString(R.string.pref_distance_units),
        mapOf(
            "meters" to DistanceUnits.Meters,
            "feet_miles" to DistanceUnits.Feet
        ),
        if (isMetricPreferred) DistanceUnits.Meters else DistanceUnits.Feet,
        saveDefault = true
    )

    val weightUnits by StringEnumPreference(
        cache,
        getString(R.string.pref_weight_units),
        mapOf(
            "kg" to WeightUnits.Kilograms,
            "lbs" to WeightUnits.Pounds
        ),
        if (isMetricPreferred) WeightUnits.Kilograms else WeightUnits.Pounds,
        saveDefault = true
    )

    val baseDistanceUnits: com.kylecorry.sol.units.DistanceUnits
        get() = if (distanceUnits == DistanceUnits.Meters) {
            com.kylecorry.sol.units.DistanceUnits.Meters
        } else {
            com.kylecorry.sol.units.DistanceUnits.Feet
        }

    val pressureUnits by StringEnumPreference(
        cache,
        getString(R.string.pref_pressure_units),
        mapOf(
            "hpa" to PressureUnits.Hpa,
            "in" to PressureUnits.Inhg,
            "mbar" to PressureUnits.Mbar,
            "psi" to PressureUnits.Psi,
            "mm" to PressureUnits.MmHg
        ),
        if (isMetricPreferred) PressureUnits.Hpa else PressureUnits.Inhg,
        saveDefault = true
    )

    val temperatureUnits by StringEnumPreference(
        cache,
        getString(R.string.pref_temperature_units),
        mapOf(
            "c" to TemperatureUnits.C,
            "f" to TemperatureUnits.F
        ),
        if (isMetricPreferred) TemperatureUnits.C else TemperatureUnits.F,
        saveDefault = true
    )

    val use24HourTime by BooleanPreference(
        cache,
        getString(R.string.pref_use_24_hour),
        Resources.uses24HourClock(context),
        saveDefault = true
    )

    val addLeadingZeroToTime by BooleanPreference(
        cache,
        getString(R.string.pref_include_leading_zero),
        false
    )

    var lastTheme: Theme by StringEnumPreference(
        cache,
        "pref_last_theme",
        mapOf(
            "light" to Theme.Light,
            "dark" to Theme.Dark,
            "black" to Theme.Black,
            "sunrise_sunset" to Theme.SunriseSunset,
            "night" to Theme.Night,
            "system" to Theme.System,
            "system_black" to Theme.SystemBlack
        ),
        Theme.System
    )

    private var _theme: Theme by StringEnumPreference(
        cache,
        context.getString(R.string.pref_theme),
        mapOf(
            "light" to Theme.Light,
            "dark" to Theme.Dark,
            "black" to Theme.Black,
            "sunrise_sunset" to Theme.SunriseSunset,
            "night" to Theme.Night,
            "system" to Theme.System,
            "system_black" to Theme.SystemBlack
        ),
        Theme.System
    )

    var theme: Theme
        get() {
            if (isLowPowerModeOn) {
                return Theme.Black
            }

            return _theme
        }
        set(value) {
            _theme = value
        }

    val useDynamicColors by BooleanPreference(
        cache,
        context.getString(R.string.pref_use_dynamic_colors),
        false
    )

    val useDynamicColorsOnCompass by BooleanPreference(
        cache,
        context.getString(R.string.pref_use_dynamic_colors_on_compass),
        false
    )

    var useCompactMode by BooleanPreference(
        cache,
        context.getString(R.string.pref_use_compact_mode),
        false
    )

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
                if (useAutoAltitude && weather.hasBarometer) {
                    raw = "gps_barometer"
                } else if (useAutoAltitude) {
                    raw = "gps"
                }
            }

            return when (raw) {
                "gps" -> AltimeterMode.GPS
                "gps_barometer" -> AltimeterMode.GPSBarometer
                "barometer" -> AltimeterMode.Barometer
                "dem" -> AltimeterMode.DigitalElevationModel
                "dem_barometer" -> AltimeterMode.DigitalElevationModelBarometer
                else -> AltimeterMode.Override
            }

        }

    var altimeterSamples: Int by IntPreference(
        cache,
        context.getString(R.string.pref_altimeter_accuracy),
        4
    )

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
            return cache.getDuration(getString(R.string.pref_backtrack_frequency))
                ?: Duration.ofMinutes(15)
        }
        set(value) {
            cache.putDuration(getString(R.string.pref_backtrack_frequency), value)
        }

    var isLowPowerModeOn: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_low_power_mode)) ?: false
        set(value) = cache.putBoolean(context.getString(R.string.pref_low_power_mode), value)

    val lowPowerModeDisablesWeather: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_low_power_mode_weather)) ?: true

    val lowPowerModeDisablesBacktrack: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_low_power_mode_backtrack)) ?: true

    val mapSite: MapSite by StringEnumPreference(
        cache, context.getString(R.string.pref_map_url_source), mapOf(
            "apple" to MapSite.Apple,
            "bing" to MapSite.Bing,
            "caltopo" to MapSite.Caltopo,
            "google" to MapSite.Google,
            "osm" to MapSite.OSM
        ), MapSite.OSM
    )

    var toolSort: ToolSortType by IntEnumPreference(
        cache,
        context.getString(R.string.pref_tool_sort),
        mapOf(
            1 to ToolSortType.Name,
            2 to ToolSortType.Category
        ),
        ToolSortType.Category
    )

    var toolQuickActions: List<Int>
        get() {
            return cache.getIntArray(context.getString(R.string.pref_tool_quick_actions))
                ?: listOf(
                    Tools.QUICK_ACTION_FLASHLIGHT,
                    Tools.QUICK_ACTION_WHISTLE,
                    Tools.QUICK_ACTION_LOW_POWER_MODE,
                )
        }
        set(value) {
            cache.putIntArray(
                context.getString(R.string.pref_tool_quick_actions),
                value
            )
        }

    var bottomNavigationTools: List<Long>
        get() {
            val maxLength = CustomBottomNavigationView.MAX_ITEM_COUNT - 1
            return (cache.getIntArray(context.getString(R.string.pref_bottom_navigation_tools))
                ?.map { it.toLong() }
                ?: listOf(
                    Tools.NAVIGATION,
                    Tools.WEATHER,
                    Tools.ASTRONOMY
                ))
                .padRight(maxLength, 0)
                .subList(0, maxLength)
        }
        set(value) {
            cache.putIntArray(
                context.getString(R.string.pref_bottom_navigation_tools),
                value.map { it.toInt() }
            )
        }

    var toolPinnedIds: List<Long>
        get() {
            return cache.getLongArray(context.getString(R.string.pref_pinned_tools)) ?: listOf(
                Tools.NAVIGATION,
                Tools.WEATHER,
                Tools.ASTRONOMY,
                Tools.USER_GUIDE
            )
        }
        set(value) {
            cache.putLongArray(context.getString(R.string.pref_pinned_tools), value)
        }

    var toolWidgets: List<String>
        get() {
            val value = cache.getString(context.getString(R.string.pref_tool_widgets))?.split(",")
            if (value != null) {
                return value
            }

            // The user hasn't made a selection, so default to all widgets
            return Tools.getTools(context)
                .flatMap { it.widgets }
                .filter { it.isEnabled(context) && it.canPlaceInApp }
                .map { it.id }
                .distinct()
        }
        set(value) {
            cache.putString(context.getString(R.string.pref_tool_widgets), value.joinToString(","))
        }

    val notificationGroupingBehavior by StringEnumPreference(
        cache,
        context.getString(R.string.pref_notification_group_behavior),
        mapOf(
            "1" to NotificationSubsystem.GroupBehavior.System,
            "2" to NotificationSubsystem.GroupBehavior.UngroupAll,
            "3" to NotificationSubsystem.GroupBehavior.UngroupHigh
        ),
        NotificationSubsystem.GroupBehavior.System
    )

    private fun getString(id: Int): String {
        return context.getString(id)
    }

    enum class DistanceUnits {
        Meters, Feet
    }

    enum class Theme {
        Light, Dark, Black, System, SunriseSunset, Night, SystemBlack
    }

    enum class AltimeterMode {
        GPS,
        GPSBarometer,
        Barometer,
        DigitalElevationModel,
        DigitalElevationModelBarometer,
        Override
    }

}