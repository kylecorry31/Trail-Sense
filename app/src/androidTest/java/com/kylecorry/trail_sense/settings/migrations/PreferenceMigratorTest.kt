package com.kylecorry.trail_sense.settings.migrations

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.andromeda.preferences.Preference
import com.kylecorry.andromeda.preferences.getIntArray
import com.kylecorry.andromeda.preferences.putIntArray
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.AppState
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dem.colors.ElevationColorStrategy
import com.kylecorry.trail_sense.shared.dem.map_layers.ElevationMapTileSource
import com.kylecorry.trail_sense.shared.dem.map_layers.HillshadeMapTileSource
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceRepo
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.gps.CacheGPSModule
import com.kylecorry.trail_sense.shared.sensors.altimeter.CachingAltimeterWrapper
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.AstronomyDailyWorker
import com.kylecorry.trail_sense.tools.map.MapToolRegistration
import com.kylecorry.trail_sense.tools.map.map_layers.BaseMapTileSource
import com.kylecorry.trail_sense.tools.navigation.NavigationToolRegistration
import com.kylecorry.trail_sense.tools.offline_maps.map_layers.TrailMapsTileSource
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.test_utils.TestUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class PreferenceMigratorTest {

    private val context = TestUtils.context
    private lateinit var prefs: IPreferences
    private var snapshot: Collection<Preference> = emptyList()

    @Before
    fun setUp() {
        prefs = PreferencesSubsystem.getInstance(context).preferences
        snapshot = prefs.getAll().toList()
        prefs.clear()
        AppState.isReturningUser = false
    }

    @After
    fun tearDown() {
        prefs.clear()
        prefs.putAll(snapshot, clearOthers = true)
        AppState.isReturningUser = false
    }

    @Test
    fun migration0To1RemovesExperimentalPreferences() {
        prefs.putBoolean("pref_enable_experimental", true)
        prefs.putBoolean("pref_use_camera_features", true)

        migrate(0)

        assertFalse(prefs.contains("pref_enable_experimental"))
        assertFalse(prefs.contains("pref_use_camera_features"))
    }

    @Test
    fun migration0To1KeepsCameraFeaturesWhenExperimentalWasNeverSet() {
        prefs.putBoolean("pref_use_camera_features", true)

        migrate(0)

        assertTrue(prefs.contains("pref_use_camera_features"))
    }

    @Test
    fun migration2To3RemovesThePressureSetpointCache() {
        val keys = listOf(
            "cache_pressure_setpoint",
            "cache_pressure_setpoint_altitude",
            "cache_pressure_setpoint_temperature",
            "cache_pressure_setpoint_time"
        )
        keys.forEach { prefs.putFloat(it, 1f) }

        migrate(2)

        keys.forEach { assertFalse(it, prefs.contains(it)) }
    }

    @Test
    fun migration3To4ConvertsThePathColorToALong() {
        val key = context.getString(R.string.pref_backtrack_path_color)
        prefs.putInt(key, 12345)

        migrate(3)

        assertEquals(12345L, prefs.getLong(key))
    }

    @Test
    fun migration4To5RemovesTheWaypointStyle() {
        prefs.putString("pref_path_waypoint_style", "dot")

        migrate(4)

        assertFalse(prefs.contains("pref_path_waypoint_style"))
    }

    @Test
    fun migration5To6RemovesTheExperimentalBarometerPreferences() {
        val keys = listOf(
            "pref_experimental_barometer_calibration",
            "pref_sea_level_require_dwell",
            "pref_barometer_altitude_change",
            "pref_sea_level_pressure_change_thresh",
            "pref_sea_level_use_rapid"
        )
        keys.forEach { prefs.putString(it, "1") }

        migrate(5)

        keys.forEach { assertFalse(it, prefs.contains(it)) }
    }

    @Test
    fun migration6To7ConvertsTheOdometerDistanceToSteps() {
        UserPreferences(context).pedometer.stepLength =
            com.kylecorry.sol.units.Distance.meters(0.5f)
        prefs.putFloat("odometer_distance", 100f)
        prefs.putString("last_odometer_location", "0,0")

        migrate(6)

        assertEquals(200L, prefs.getLong("cache_steps"))
        assertFalse(prefs.contains("odometer_distance"))
        assertFalse(prefs.contains("last_odometer_location"))
    }

    // Not testing the happy path because it depends on screen dpi
    @Test
    fun migration7To8LeavesAnUncalibratedRulerAlone() {
        val ruler = UserPreferences(context).ruler
        ruler.rulerScale = 1f

        migrate(7)

        assertEquals(1f, ruler.rulerScale, 0.0001f)
    }

    @Test
    fun migration8To9ConvertsTheUpdateFrequenciesToDurations() {
        prefs.putString("pref_backtrack_frequency", "30")
        prefs.putString("pref_weather_update_frequency", "45")

        migrate(8)

        val userPrefs = UserPreferences(context)
        assertEquals(Duration.ofMinutes(30), userPrefs.backtrackRecordFrequency)
        assertEquals(Duration.ofMinutes(45), userPrefs.weather.weatherUpdateFrequency)
    }

    @Test
    fun migration9To10SetsPressureSmoothingWhenTheV2CalibrationWasOff() {
        val keys = listOf(
            "pref_barometer_altitude_outlier",
            "pref_barometer_altitude_smoothing",
            "pref_experimental_sea_level_calibration_v2"
        )
        prefs.putString("pref_barometer_altitude_outlier", "1")
        prefs.putString("pref_barometer_altitude_smoothing", "1")
        prefs.putBoolean("pref_experimental_sea_level_calibration_v2", false)

        migrate(9)

        assertEquals(15f, UserPreferences(context).weather.pressureSmoothing, 0.0001f)
        keys.forEach { assertFalse(it, prefs.contains(it)) }
    }

    @Test
    fun migration10To11MovesTheAstronomyAlertRunDate() {
        val date = LocalDate.of(2024, 1, 2)
        prefs.putLocalDate("pref_astronomy_alerts_last_run_date", date)

        migrate(10)

        assertEquals(
            date,
            prefs.getLocalDate(
                "pref_andromeda_daily_worker_last_run_date_${AstronomyDailyWorker.UNIQUE_ID}"
            )
        )
        assertFalse(prefs.contains("pref_astronomy_alerts_last_run_date"))
    }

    @Test
    fun migration11To12MovesTheLastAltitudeToTheAltimeterCache() {
        prefs.putFloat(CacheGPSModule.LAST_ALTITUDE, 123f)

        migrate(11)

        assertEquals(123f, prefs.getFloat(CachingAltimeterWrapper.LAST_ALTITUDE_KEY)!!, 0.0001f)
    }

    @Test
    fun migration12To13ResetsTheThermometerCalibration() {
        val thermometer = UserPreferences(context).thermometer
        thermometer.minActualTemperature = 5f
        thermometer.maxActualTemperature = 50f

        migrate(12)

        assertEquals(0f, thermometer.minActualTemperature, 0.0001f)
        assertEquals(100f, thermometer.maxActualTemperature, 0.0001f)
    }

    @Test
    fun migration13To14RemovesTheLegacyCompassPreference() {
        prefs.putBoolean("pref_use_legacy_compass_2", true)

        migrate(13)

        assertFalse(prefs.contains("pref_use_legacy_compass_2"))
        // The source is whatever this device supports, so only the fact that one was chosen is checked
        assertTrue(prefs.contains(context.getString(R.string.pref_compass_source)))
    }

    @Test
    fun migration14To15WritesTheImplicitUnitDefaultsToDisk() {
        val unitKeys = listOf(
            R.string.pref_use_24_hour,
            R.string.pref_distance_units,
            R.string.pref_weight_units,
            R.string.pref_pressure_units,
            R.string.pref_temperature_units
        ).map { context.getString(it) }
        // An explicit choice must survive the defaults being written
        prefs.putString(context.getString(R.string.pref_distance_units), "meters")
        unitKeys.filter { it != context.getString(R.string.pref_distance_units) }
            .forEach { assertFalse(it, prefs.contains(it)) }

        migrate(14)

        unitKeys.forEach { assertTrue(it, prefs.contains(it)) }
        assertEquals("meters", prefs.getString(context.getString(R.string.pref_distance_units)))
    }

    @Test
    fun migration15To16EnablesCliffHeightWhenItWasUsed() {
        prefs.putBoolean("cache_dialog_tool_cliff_height", true)

        migrate(15)

        assertTrue(UserPreferences(context).isCliffHeightEnabled)
    }

    @Test
    fun migration15To16LeavesCliffHeightDisabledWhenItWasNeverUsed() {
        UserPreferences(context).isCliffHeightEnabled = false

        migrate(15)

        assertFalse(UserPreferences(context).isCliffHeightEnabled)
    }

    @Test
    fun migration16To17ReplacesTheOldToolQuickActions() {
        val leftKey = context.getString(R.string.pref_navigation_quick_action_left)
        val arrayKey = context.getString(R.string.pref_tool_quick_actions)
        prefs.putString(leftKey, "7")
        prefs.putIntArray(arrayKey, listOf(0, 99))

        migrate(16)

        val offlineMaps = Tools.OFFLINE_MAPS.toInt() + Tools.TOOL_QUICK_ACTION_OFFSET
        val paths = Tools.PATHS.toInt() + Tools.TOOL_QUICK_ACTION_OFFSET
        assertEquals(offlineMaps.toString(), prefs.getString(leftKey))
        // 99 is not a legacy tool action, so it is left as-is
        assertEquals(listOf(paths, 99), prefs.getIntArray(arrayKey))
    }

    @Test
    fun migration17To18DisablesTheMapLayerForReturningUsersOnly() {
        AppState.isReturningUser = true
        migrate(17)
        assertEquals(false, prefs.getBoolean("pref_navigation_map_layer_enabled"))

        AppState.isReturningUser = false
        migrate(17)
        assertEquals(true, prefs.getBoolean("pref_navigation_map_layer_enabled"))
    }

    @Test
    fun migration18To19MovesContourElevationColoringToTheColorStrategy() {
        prefs.putBoolean("pref_navigation_contour_layer_color_with_elevation", true)
        prefs.putBoolean("pref_map_contour_layer_color_with_elevation", false)

        migrate(18)

        assertEquals(
            ElevationColorStrategy.Vibrant.id.toString(),
            prefs.getString("pref_navigation_contour_layer_color")
        )
        assertNull(prefs.getString("pref_map_contour_layer_color"))
    }

    @Test
    fun migration19To20RemovesTheLastDestinationBearing() {
        prefs.putFloat("last_dest_bearing", 90f)

        migrate(19)

        // The navigator is updated asynchronously, so only the removal is checked here
        assertFalse(prefs.contains("last_dest_bearing"))
    }

    @Test
    fun migration20To21DisablesNewLayersWithoutOverridingChoices() {
        prefs.putBoolean("pref_navigation_elevation_layer_enabled", true)

        migrate(20)

        assertEquals(true, prefs.getBoolean("pref_navigation_elevation_layer_enabled"))
        assertEquals(false, prefs.getBoolean("pref_navigation_hillshade_layer_enabled"))
        assertEquals(50, prefs.getInt("pref_navigation_elevation_layer_opacity"))
    }

    @Test
    fun migration21To22DisablesTheSlopeLayersByDefault() {
        prefs.putBoolean("pref_map_slope_layer_enabled", true)

        migrate(21)

        assertEquals(true, prefs.getBoolean("pref_map_slope_layer_enabled"))
        assertEquals(false, prefs.getBoolean("pref_navigation_slope_layer_enabled"))
        assertEquals(false, prefs.getBoolean("pref_photo_maps_slope_layer_enabled"))
    }

    @Test
    fun migration22To23DisablesTheAspectLayersByDefault() {
        prefs.putBoolean("pref_map_aspect_layer_enabled", true)

        migrate(22)

        assertEquals(true, prefs.getBoolean("pref_map_aspect_layer_enabled"))
        assertEquals(false, prefs.getBoolean("pref_navigation_aspect_layer_enabled"))
        assertEquals(false, prefs.getBoolean("pref_photo_maps_aspect_layer_enabled"))
    }

    @Test
    fun migration23To24MovesEnabledLayersIntoTheLayerRepo() {
        prefs.putBoolean("pref_navigation_hillshade_layer_enabled", false)

        migrate(23)

        val active = getAppService<MapLayerPreferenceRepo>().getActiveLayerIds("navigation")
        assertTrue(active.contains(BaseMapTileSource.SOURCE_ID))
        assertFalse(active.contains(HillshadeMapTileSource.SOURCE_ID))
    }

    @Test
    fun migration24To25MovesTheNearbyDistanceToTheViewDistance() {
        prefs.putString(context.getString(R.string.pref_max_beacon_distance), "5000")

        migrate(24)

        assertEquals("5000", prefs.getString("pref_navigation_view_distance"))
        assertEquals(true, prefs.getBoolean(context.getString(R.string.pref_nearby_linear_only)))
    }

    @Test
    fun migration25To26ConvertsTheViewDistanceToARadarScale() {
        prefs.putString("pref_navigation_view_distance", "2")

        migrate(25)

        val scale = prefs.getFloat("cache_radar_compass_state_scale")!!
        assertTrue("Expected a positive scale but was $scale", scale > 0f)
        assertFalse(prefs.contains("pref_navigation_view_distance"))
    }

    @Test
    fun migration26To27AddsOfflineMapsBelowElevation() {
        val repo = getAppService<MapLayerPreferenceRepo>()
        val mapId = NavigationToolRegistration.MAP_ID
        repo.setActiveLayerIds(
            mapId,
            listOf(
                BaseMapTileSource.SOURCE_ID,
                ElevationMapTileSource.SOURCE_ID,
                HillshadeMapTileSource.SOURCE_ID
            )
        )
        repo.setActiveLayerIds(MapToolRegistration.MAP_ID, listOf(BaseMapTileSource.SOURCE_ID))

        migrate(26)

        val active = repo.getActiveLayerIds(mapId)
        assertTrue(active.contains(TrailMapsTileSource.SOURCE_ID))
        assertTrue(
            "Offline maps must sit above elevation: $active",
            active.indexOf(TrailMapsTileSource.SOURCE_ID) >
                    active.indexOf(ElevationMapTileSource.SOURCE_ID)
        )
        assertTrue(
            repo.getActiveLayerIds(MapToolRegistration.MAP_ID)
                .contains(TrailMapsTileSource.SOURCE_ID)
        )
        assertEquals(
            50,
            prefs.getInt("pref_${mapId}_${TrailMapsTileSource.SOURCE_ID}_layer_opacity")
        )
    }

    @Test
    fun migration27To28HidesNavigationLabelsForReturningUsersOnly() {
        val key = context.getString(R.string.pref_use_bottom_navigation_labels)

        AppState.isReturningUser = false
        migrate(27)
        assertFalse(prefs.contains(key))

        AppState.isReturningUser = true
        migrate(27)
        assertEquals(false, prefs.getBoolean(key))
    }

    // Migration 28 -> 29 is tested separately since it kicks off a background service

    @Test
    fun migration29To30OptsReturningUsersOutOfTheDailyOdometerReset() {
        val key = context.getString(R.string.pref_odometer_reset_daily)
        AppState.isReturningUser = true

        migrate(29)

        assertEquals(false, prefs.getBoolean(key))
    }

    @Test
    fun migration29To30LeavesNewUsersOnTheDefault() {
        val key = context.getString(R.string.pref_odometer_reset_daily)
        AppState.isReturningUser = false

        migrate(29)

        assertFalse(prefs.contains(key))
    }

    @Test
    fun migration29To30DoesNotOverrideAnExplicitChoice() {
        val key = context.getString(R.string.pref_odometer_reset_daily)
        AppState.isReturningUser = true
        prefs.putBoolean(key, true)

        migrate(29)

        assertEquals(true, prefs.getBoolean(key))
    }

    private fun migrate(fromVersion: Int) {
        val migration = PreferenceMigrator.migrations.first { it.fromVersion == fromVersion }
        migration.action(context, prefs)
    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            TestUtils.setupApplication()
        }
    }
}
