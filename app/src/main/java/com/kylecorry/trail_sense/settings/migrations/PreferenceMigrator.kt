package com.kylecorry.trail_sense.settings.migrations

import android.content.Context
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.Screen
import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.andromeda.preferences.getIntArray
import com.kylecorry.andromeda.preferences.putIntArray
import com.kylecorry.luna.text.toIntCompat
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.AppState
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dem.colors.ElevationColorStrategy
import com.kylecorry.trail_sense.shared.dem.map_layers.AspectMapTileSource
import com.kylecorry.trail_sense.shared.dem.map_layers.ContourGeoJsonSource
import com.kylecorry.trail_sense.shared.dem.map_layers.ElevationMapTileSource
import com.kylecorry.trail_sense.shared.dem.map_layers.HillshadeMapTileSource
import com.kylecorry.trail_sense.shared.dem.map_layers.SlopeMapTileSource
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceRepo
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.CustomGPS
import com.kylecorry.trail_sense.shared.sensors.altimeter.CachingAltimeterWrapper
import com.kylecorry.trail_sense.shared.sensors.compass.CompassSource
import com.kylecorry.trail_sense.shared.sensors.providers.CompassProvider
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.AstronomyDailyWorker
import com.kylecorry.trail_sense.tools.beacons.map_layers.BeaconGeoJsonSource
import com.kylecorry.trail_sense.tools.map.map_layers.BaseMapTileSource
import com.kylecorry.trail_sense.tools.map.map_layers.MyLocationGeoJsonSource
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.map_layers.NavigationGeoJsonSource
import com.kylecorry.trail_sense.tools.paths.map_layers.PathGeoJsonSource
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounter
import com.kylecorry.trail_sense.tools.photo_maps.map_layers.PhotoMapTileSource
import com.kylecorry.trail_sense.tools.signal_finder.map_layers.CellTowerGeoJsonSource
import com.kylecorry.trail_sense.tools.tides.map_layers.TideGeoJsonSource
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration

class PreferenceMigrator private constructor() {

    private val lock = Any()

    fun migrate(context: Context) {
        synchronized(lock) {
            val prefs = PreferencesSubsystem.getInstance(context).preferences
            var currentVersion = prefs.getInt("pref_version") ?: 0

            AppState.isReturningUser = currentVersion > 0

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
        private val staticLock = Any()

        private const val version = 24
        private val migrations = listOf(
            PreferenceMigration(0, 1) { _, prefs ->
                if (prefs.contains("pref_enable_experimental")) {
                    prefs.remove("pref_enable_experimental")
                    prefs.remove("pref_use_camera_features")
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
                    val stride = UserPreferences(context).pedometer.strideLength.meters().value
                    if (stride > 0f) {
                        val steps = (distance / stride).toLong()
                        prefs.putLong(StepCounter.STEPS_KEY, steps)
                    }
                }
                prefs.remove("odometer_distance")
                prefs.remove("last_odometer_location")
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
            },
            PreferenceMigration(9, 10) { context, prefs ->
                if (prefs.getBoolean("pref_experimental_sea_level_calibration_v2") != true) {
                    val userPreferences = UserPreferences(context)
                    userPreferences.weather.pressureSmoothing = 15f
                }

                prefs.remove("pref_barometer_altitude_outlier")
                prefs.remove("pref_barometer_altitude_smoothing")
                prefs.remove("pref_experimental_sea_level_calibration_v2")
            },
            PreferenceMigration(10, 11) { _, prefs ->
                val date = prefs.getLocalDate("pref_astronomy_alerts_last_run_date")
                if (date != null) {
                    prefs.putLocalDate(
                        "pref_andromeda_daily_worker_last_run_date_${AstronomyDailyWorker.UNIQUE_ID}",
                        date
                    )
                }
                prefs.remove("pref_astronomy_alerts_last_run_date")
            },
            PreferenceMigration(11, 12) { _, prefs ->
                val elevation = prefs.getFloat(CustomGPS.LAST_ALTITUDE)
                if (elevation != null) {
                    prefs.putFloat(CachingAltimeterWrapper.LAST_ALTITUDE_KEY, elevation)
                }
            },
            PreferenceMigration(12, 13) { context, _ ->
                val userPrefs = UserPreferences(context)
                userPrefs.thermometer.resetThermometerCalibration()
            },
            PreferenceMigration(13, 14) { context, prefs ->
                val userPrefs = UserPreferences(context)
                val wasLegacyCompass = prefs.getBoolean("pref_use_legacy_compass_2") ?: false
                val sources = CompassProvider.getAvailableSources(context)
                if (wasLegacyCompass) {
                    userPrefs.compass.source = CompassSource.Orientation
                } else if (sources.contains(CompassSource.RotationVector)) {
                    // The rotation vector is accurate, no need for smoothing
                    userPrefs.compass.compassSmoothing = 1
                }
                userPrefs.compass.source = sources.firstOrNull() ?: CompassSource.CustomMagnetometer
                prefs.remove("pref_use_legacy_compass_2")
            },
            PreferenceMigration(14, 15) { context, _ ->
                val userPrefs = UserPreferences(context)

                // By grabbing the preferences, it will solidify the defaults
                userPrefs.use24HourTime
                userPrefs.distanceUnits
                userPrefs.weightUnits
                userPrefs.pressureUnits
                userPrefs.temperatureUnits
            },
            PreferenceMigration(15, 16) { context, prefs ->
                if (prefs.getBoolean("cache_dialog_tool_cliff_height") != null) {
                    // Enable the cliff height tool since it was previously used
                    val userPrefs = UserPreferences(context)
                    userPrefs.isCliffHeightEnabled = true
                }
            },
            PreferenceMigration(16, 17) { context, prefs ->
                // Replace the old tool quick actions with the generic tool quick action
                val individualQuickActionPrefs = listOf(
                    context.getString(R.string.pref_navigation_quick_action_left),
                    context.getString(R.string.pref_navigation_quick_action_right),
                    context.getString(R.string.pref_astronomy_quick_action_left),
                    context.getString(R.string.pref_astronomy_quick_action_right),
                    context.getString(R.string.pref_weather_quick_action_left),
                    context.getString(R.string.pref_weather_quick_action_right),
                )
                val toolQuickActionPrefs = context.getString(R.string.pref_tool_quick_actions)

                val replacementMap = mapOf(
                    7 to Tools.PHOTO_MAPS.toInt() + Tools.TOOL_QUICK_ACTION_OFFSET,
                    0 to Tools.PATHS.toInt() + Tools.TOOL_QUICK_ACTION_OFFSET,
                    12 to Tools.CLIMATE.toInt() + Tools.TOOL_QUICK_ACTION_OFFSET,
                    3 to Tools.TEMPERATURE_ESTIMATION.toInt() + Tools.TOOL_QUICK_ACTION_OFFSET,
                    2 to Tools.CLOUDS.toInt() + Tools.TOOL_QUICK_ACTION_OFFSET,
                    11 to Tools.LIGHTNING_STRIKE_DISTANCE.toInt() + Tools.TOOL_QUICK_ACTION_OFFSET
                )

                for (pref in individualQuickActionPrefs) {
                    val tool = prefs.getString(pref)?.toIntCompat()
                    if (tool != null) {
                        val replacement = replacementMap[tool]
                        if (replacement != null) {
                            prefs.putString(pref, replacement.toString())
                        }
                    }
                }

                val toolQuickActions = prefs.getIntArray(toolQuickActionPrefs)
                if (toolQuickActions != null) {
                    val newToolQuickActions = toolQuickActions.mapNotNull {
                        if (it in replacementMap) {
                            replacementMap[it]
                        } else {
                            it
                        }
                    }
                    prefs.putIntArray(toolQuickActionPrefs, newToolQuickActions)
                }
            },
            PreferenceMigration(17, 18) { context, prefs ->
                // Disable the map layer by default for returning users to not be disruptive
                val key = "pref_navigation_map_layer_enabled"
                prefs.putBoolean(key, !AppState.isReturningUser)
            },
            PreferenceMigration(18, 19) { context, prefs ->
                val mapIds = listOf("navigation", "map", "photo_maps")
                for (mapId in mapIds) {
                    if (prefs.getBoolean("pref_${mapId}_contour_layer_color_with_elevation") == true) {
                        prefs.putString(
                            "pref_${mapId}_contour_layer_color",
                            ElevationColorStrategy.Vibrant.id.toString()
                        )
                    }
                }
            },
            PreferenceMigration(19, 20) { context, prefs ->
                val bearing = prefs.getFloat("last_dest_bearing")
                prefs.remove("last_dest_bearing")
                if (bearing != null) {
                    val scope = CoroutineScope(Dispatchers.IO)
                    scope.launch {
                        val navigator = Navigator.getInstance(context)
                        navigator.navigateToBearing(bearing)
                    }
                }
            },
            PreferenceMigration(20, 21) { context, prefs ->

                val disabledByDefault = listOf(
                    // Elevation
                    "pref_photo_maps_elevation_layer_enabled",
                    "pref_navigation_elevation_layer_enabled",
                    // Hillshade
                    "pref_photo_maps_hillshade_layer_enabled",
                    "pref_navigation_hillshade_layer_enabled",
                    // Contour
                    "pref_photo_maps_contour_layer_enabled",
                    "pref_navigation_contour_layer_enabled",
                    // Base map
                    "pref_photo_maps_base_map_layer_enabled",
                    "pref_navigation_base_map_layer_enabled",
                    // Cell towers
                    "pref_map_cell_tower_layer_enabled",
                    "pref_photo_maps_cell_tower_layer_enabled",
                    "pref_navigation_cell_tower_layer_enabled",
                    // Navigation
                    "pref_navigation_navigation_layer_enabled",
                )

                for (key in disabledByDefault) {
                    if (!prefs.contains(key)) {
                        prefs.putBoolean(key, false)
                    }
                }

                val halfOpacityByDefault = listOf(
                    // Elevation
                    "pref_map_elevation_layer_opacity",
                    "pref_photo_maps_elevation_layer_opacity",
                    "pref_navigation_elevation_layer_opacity",
                    // Hillshade
                    "pref_map_hillshade_layer_opacity",
                    "pref_photo_maps_hillshade_layer_opacity",
                    "pref_navigation_hillshade_layer_opacity",
                    // Contour
                    "pref_map_contour_layer_opacity",
                    "pref_photo_maps_contour_layer_opacity",
                    "pref_navigation_contour_layer_opacity",
                    // Photo Maps
                    "pref_navigation_map_layer_opacity",
                )

                for (key in halfOpacityByDefault) {
                    if (!prefs.contains(key)) {
                        prefs.putInt(key, 50)
                    }
                }
            },
            PreferenceMigration(21, 22) { _, prefs ->
                val disabledByDefault = listOf(
                    "pref_map_slope_layer_enabled",
                    "pref_photo_maps_slope_layer_enabled",
                    "pref_navigation_slope_layer_enabled"
                )

                for (key in disabledByDefault) {
                    if (!prefs.contains(key)) {
                        prefs.putBoolean(key, false)
                    }
                }
            },
            PreferenceMigration(22, 23) { _, prefs ->
                val disabledByDefault = listOf(
                    "pref_map_aspect_layer_enabled",
                    "pref_photo_maps_aspect_layer_enabled",
                    "pref_navigation_aspect_layer_enabled"
                )

                for (key in disabledByDefault) {
                    if (!prefs.contains(key)) {
                        prefs.putBoolean(key, false)
                    }
                }
            },
            PreferenceMigration(23, 24) { _, prefs ->
                val repo = AppServiceRegistry.get<MapLayerPreferenceRepo>()
                val allLayers = listOf(
                    BaseMapTileSource.SOURCE_ID,
                    ElevationMapTileSource.SOURCE_ID,
                    HillshadeMapTileSource.SOURCE_ID,
                    AspectMapTileSource.SOURCE_ID,
                    SlopeMapTileSource.SOURCE_ID,
                    PhotoMapTileSource.SOURCE_ID,
                    ContourGeoJsonSource.SOURCE_ID,
                    NavigationGeoJsonSource.SOURCE_ID,
                    CellTowerGeoJsonSource.SOURCE_ID,
                    TideGeoJsonSource.SOURCE_ID,
                    PathGeoJsonSource.SOURCE_ID,
                    BeaconGeoJsonSource.SOURCE_ID,
                    MyLocationGeoJsonSource.SOURCE_ID,
                )

                // Navigation
                repo.setActiveLayerIds(
                    "navigation", allLayers.filter {
                        isMapLayerEnabled(prefs, "navigation", it)
                    }
                )

                // Map
                repo.setActiveLayerIds(
                    "map", allLayers.filter {
                        isMapLayerEnabled(prefs, "map", it)
                    }
                )

                // Photo Map
                repo.setActiveLayerIds(
                    "photo_maps", allLayers.filter {
                        it == PhotoMapTileSource.SOURCE_ID || isMapLayerEnabled(
                            prefs,
                            "photo_maps",
                            it
                        )
                    }
                )
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

        private fun isMapLayerEnabled(
            prefs: IPreferences,
            mapId: String,
            layerId: String
        ): Boolean {
            val key = "pref_${mapId}_${layerId}_layer_enabled"
            return prefs.getBoolean(key) ?: true
        }


    }

}
