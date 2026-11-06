package com.kylecorry.trail_sense.screenshots

import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.luna.result.Result
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dem.map_layers.ContourGeoJsonSource
import com.kylecorry.trail_sense.shared.dem.map_layers.HillshadeMapTileSource
import com.kylecorry.trail_sense.shared.extensions.findNavController
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.DefaultMapLayerDefinitions
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceRepo
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOkIfShown
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.delay
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestData
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.TestUtils.finishCurrentActivity
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.tools.astronomy.map_layers.NightTileSource
import com.kylecorry.trail_sense.tools.navigation.NavigationToolRegistration
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathFollow
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.offline_maps.domain.CreateOfflineMapRequest
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapService
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.beacons.map_layers.BeaconGeoJsonSource
import com.kylecorry.trail_sense.tools.map.MapToolRegistration
import com.kylecorry.trail_sense.tools.map.map_layers.BaseMapTileSource
import com.kylecorry.trail_sense.tools.map.map_layers.MyLocationGeoJsonSource
import com.kylecorry.trail_sense.tools.map.ui.MapView
import com.kylecorry.trail_sense.tools.navigation.map_layers.NavigationGeoJsonSource
import com.kylecorry.trail_sense.tools.offline_maps.map_layers.TrailMapsTileSource
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration
import com.kylecorry.trail_sense.tools.paths.map_layers.PathGeoJsonSource
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.tides.map_layers.TideGeoJsonSource
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import java.time.format.DateTimeFormatter

/**
 * Captures the screenshots used on the website and the store listings. Every screen is built from
 * data loaded directly into the app, so the set can be regenerated at any time.
 *
 * Before running, put the Rhode Island Mapsforge map on the device (it is too large to check in,
 * the screens that use it are captured without it if it is missing):
 *
 *     curl -O https://download.mapsforge.org/maps/v5/north-america/us/rhode-island.map
 *     adb push rhode-island.map /data/local/tmp/rhode-island.map
 *
 * Run on an emulator, since the test resizes the display and overrides the status bar. The tests are
 * skipped unless this class is named in the test filter, so full test runs leave them out:
 *
 *     scripts/run-emulator-integration-tests.sh com.kylecorry.trail_sense.screenshots.StoreScreenshotsTest
 *
 * Then collect the screenshots (a test run uninstalls the app, but these are saved to shared
 * storage so they survive):
 *
 *     adb pull /sdcard/Pictures/TrailSenseStoreScreenshots
 *
 * The photo map is the Glocester Land Trust's trail map of Steere Hill Farm (a georeferenced PDF),
 * and the navigation hero uses a real hike of Tuckerman Ravine on Mount Washington.
 */
class StoreScreenshotsTest : ToolTestBase(Tools.NAVIGATION, STEERE_HILL) {

    private var timeShiftMillis = 0L

    override fun seedData() {
        setUpDisplay()
        setUpPreferences()
    }

    @After
    fun restoreDevice() {
        runBlocking {
            Tools.getService(context, PathsToolRegistration.SERVICE_BACKTRACK)?.disable()
        }
        restoreSystemTime()
        TestUtils.runShellCommand("wm size reset")
        TestUtils.runShellCommand("wm density reset")
        demoMode("exit")
    }

    @Test
    fun nearby() {
        open(Tools.NAVIGATION, STEERE_HILL) {
            hideCompassLayer(HillshadeMapTileSource.SOURCE_ID)
            hideCompassLayer(NightTileSource.SOURCE_ID)
            setCompassLayerOpacity(TrailMapsTileSource.SOURCE_ID, 100)
            loadTrailMap()
            NEARBY_BEACONS.forEach { (name, location, elevation) ->
                TestData.addBeacon(name, location, elevation = elevation)
            }
        }
        isVisible(R.id.radar_compass)
        capture("nearby", settle = 3000)
    }

    @Test
    fun backtrack() {
        // Stops just after the loop turns back, as if the hike were still in progress
        val recorded = TestData.loadGpxPoints("paths/durfee loop.gpx")
            .take(BACKTRACK_CURRENT_INDEX + 1)
        // Shift the hike so it looks like it was just recorded
        val shift = Duration.between(recorded.last().time, Instant.now().minusSeconds(30))
        open(Tools.PATHS, recorded.last().coordinate) {
            val paths = PathService.getInstance(context)
            runBlocking {
                recorded.forEach { paths.addBacktrackPoint(it.copy(time = it.time?.plus(shift))) }
                // Restarting (instead of enabling) keeps recording into the path loaded above
                UserPreferences(context).paths.backtrackEnabled = true
                Tools.getService(context, PathsToolRegistration.SERVICE_BACKTRACK)?.restart()
            }
        }
        click(com.kylecorry.andromeda.views.R.id.title)
        isVisible(R.id.stop_recording_btn, waitForTime = 5000)
        capture("backtrack", settle = 3000)
    }

    @Test
    fun weather() {
        open(Tools.WEATHER, STEERE_HILL) { addApproachingFrontPressure() }
        clickOkIfShown()
        hasText(R.id.weather_title, string(R.string.precipitation_rain), waitForTime = 15000)
        isVisible(R.id.chart)
        capture("weather", settle = 2000)
    }

    @Test
    fun astronomy() {
        // The sun is low and the day is nearly over
        val sunset = AstronomyService().getSunTimes(
            STEERE_HILL,
            UserPreferences(context).astronomy.sunTimesMode,
            LocalDate.now()
        ).set ?: error("The sun doesn't set at $STEERE_HILL today")
        setSystemTime(sunset.minusMinutes(30))
        open(Tools.ASTRONOMY, STEERE_HILL)
        capture("astronomy", settle = 4000)
    }

    @Test
    fun tools() {
        open(0L, STEERE_HILL)
        capture("tools", settle = 1500)
    }

    @Test
    fun maps() {
        open(Tools.MAP, STEERE_HILL) {
            loadTrailMap()
            loadPhotoMap()
        }
        clickOkIfShown()
        // Far enough out to see the trail map around the photo map
        setMapResolution(11f)
        capture("maps", settle = 6000)
    }

    @Test
    fun mapTides() {
        open(Tools.MAP, BOSTON) { showBasemapModeledTidesAndLocation() }
        clickOkIfShown()
        // Modeled tides are only drawn at zoom level 8 and closer (about 640 m per dp at this
        // latitude), this is as far out as they go
        setMapResolution(600f)
        capture("map_tides", settle = 10000)
    }

    @Test
    fun heroNavigation() {
        val tuckerman = TestData.loadGpxPoints("paths/tuckermans ravine.gpx")
        val onTheTrail = tuckerman[TUCKERMAN_CURRENT_INDEX].coordinate
        open(Tools.NAVIGATION, onTheTrail, tuckerman[TUCKERMAN_CURRENT_INDEX].elevation ?: 0f) {
            // Twice as far out as the default, to see more of the trail
            PreferencesSubsystem.getInstance(context).preferences
                .putFloat("cache_radar_compass_state_scale", 4f)
            showCompassLayers(
                BaseMapTileSource.SOURCE_ID,
                ContourGeoJsonSource.SOURCE_ID,
                NavigationGeoJsonSource.SOURCE_ID,
                PathGeoJsonSource.SOURCE_ID,
                BeaconGeoJsonSource.SOURCE_ID,
                MyLocationGeoJsonSource.SOURCE_ID
            )
            val paths = PathService.getInstance(context)
            runBlocking {
                val id = TestData.addPathFromGpx("Tuckerman Ravine", "paths/tuckermans ravine.gpx")
                val summit = paths.getWaypoints(id).sortedBy { it.id }[TUCKERMAN_SUMMIT_INDEX]
                Navigator.getInstance(context)
                    .navigateAlongPath(paths.getPath(id)!!, PathFollow.ToPoint(summit.id))
            }
        }
        isVisible(R.id.navigation_sheet_title, waitForTime = 15000)
        capture("hero_navigation", settle = 4000)
    }

    @Test
    fun heroMap() {
        val hike = TestData.loadGpxPoints("paths/durfee loop.gpx")
        open(Tools.MAP, hike[HIKE_CURRENT_INDEX].coordinate) {
            loadTrailMap()
            TestData.addPathFromGpx("Durfee Hill Loop", "paths/durfee loop.gpx")
            TestData.addBeacon("Trailhead", hike.first().coordinate, elevation = hike.first().elevation)
        }
        clickOkIfShown()
        showMapBounds(CoordinateBounds.from(hike.map { it.coordinate }))
        capture("hero_map", settle = 6000)
    }

    /**
     * Replaces the running tool with [toolId], after loading whatever [seed] adds and moving the
     * user to [location]. The tool is closed while seeding so nothing races the loaded data.
     */
    private fun open(
        toolId: Long,
        location: Coordinate,
        elevation: Float = STEERE_HILL_ELEVATION,
        seed: () -> Unit = {}
    ) {
        finishCurrentActivity()
        TestData.clear()
        runBlocking { Navigator.getInstance(context).cancelAllNavigation() }
        TestUtils.setLocationOverride(location)
        UserPreferences(context).altitudeOverride = elevation
        seed()
        scenario = TestUtils.startWithTool(toolId) { navController = it.findNavController()!! }
    }

    private fun setMapResolution(metersPerPixel: Float) {
        waitForMap()
        scenario.onActivity { it.findViewById<MapView>(R.id.map).resolution = metersPerPixel }
    }

    private fun showMapBounds(bounds: CoordinateBounds) {
        waitForMap()
        scenario.onActivity {
            // Extra padding keeps the path clear of the map's buttons
            it.findViewById<MapView>(R.id.map).fitIntoView(bounds, paddingFactor = 1.5f)
        }
    }

    private fun waitForMap() {
        isVisible(R.id.map)
        // The map restores its last position when it opens, which would undo a change made too early
        delay(1500)
    }

    private fun capture(name: String, settle: Long) {
        // Map tiles, contours, and charts draw asynchronously after the screen is idle
        delay(settle)
        TestUtils.waitForIdle()
        save(name)
    }

    private fun save(name: String) {
        // The shell owns the files, so a later run (even after a reinstall) can overwrite them
        TestUtils.runShellCommand("mkdir -p /sdcard/$SCREENSHOT_DIRECTORY")
        TestUtils.runShellCommand("screencap -p /sdcard/$SCREENSHOT_DIRECTORY/$name.png")
    }

    private fun setUpDisplay() {
        // The same pixel size and density as a modern phone, regardless of the emulator's skin
        TestUtils.runShellCommand("wm size 1080x2424")
        TestUtils.runShellCommand("wm density 420")
        demoMode("enter")
        demoMode("clock", "-e hhmm ${LocalTime.now().format(DateTimeFormatter.ofPattern("HHmm"))}")
        demoMode("battery", "-e level 100 -e plugged false")
        demoMode("network", "-e wifi show -e level 4 -e mobile hide")
        demoMode("notifications", "-e visible false")
        delay(1500)
    }

    /**
     * Moves the device's clock to [time] (the date stays whatever it is today), until the test ends.
     */
    private fun setSystemTime(time: ZonedDateTime) {
        timeShiftMillis = time.toInstant().toEpochMilli() - System.currentTimeMillis()
        TestUtils.runShellCommand("cmd time_detector set_auto_detection_enabled false")
        TestUtils.runShellCommand("cmd alarm set-time ${time.toInstant().toEpochMilli()}")
        demoMode("clock", "-e hhmm ${time.format(DateTimeFormatter.ofPattern("HHmm"))}")
    }

    private fun restoreSystemTime() {
        if (timeShiftMillis == 0L) {
            return
        }
        TestUtils.runShellCommand("cmd alarm set-time ${System.currentTimeMillis() - timeShiftMillis}")
        TestUtils.runShellCommand("cmd time_detector set_auto_detection_enabled true")
        timeShiftMillis = 0L
    }

    private fun demoMode(command: String, extras: String = "") {
        TestUtils.runShellCommand("settings put global sysui_demo_allowed 1")
        TestUtils.runShellCommand(
            "am broadcast -a com.android.systemui.demo -e command $command $extras"
        )
    }

    private fun setUpPreferences() {
        val preferences = PreferencesSubsystem.getInstance(context).preferences
        val user = UserPreferences(context)
        user.theme = UserPreferences.Theme.Dark
        user.useCompactMode = false
        user.toolPinnedIds = listOf(
            Tools.AUGMENTED_REALITY,
            Tools.BEACONS,
            Tools.FIELD_GUIDE,
            Tools.OFFLINE_MAPS
        )
        // Elevation comes from the override set when a screen is opened
        preferences.putString(context.getString(R.string.pref_altimeter_calibration_mode), "override")
        // The radar compass is shown when the phone is flat, the linear one when it is upright
        preferences.putBoolean(context.getString(R.string.pref_show_linear_compass), false)
        // The larger location indicator is easier to see in a small screenshot
        listOf(MapToolRegistration.MAP_ID, NavigationToolRegistration.MAP_ID).forEach {
            preferences.putBoolean(
                MapLayerPreferenceRepo.getPreferenceKey(
                    it,
                    MyLocationGeoJsonSource.SOURCE_ID,
                    MyLocationGeoJsonSource.LARGE_INDICATOR
                ),
                true
            )
        }
        // Degree markings around the radar compass
        preferences.putBoolean(
            context.getString(R.string.pref_navigation_draw_compass_dial_bezel),
            true
        )
        preferences.putBoolean("tools_long_press_notice_shown", true)
        // Back to the default navigation bar
        preferences.remove(context.getString(R.string.pref_bottom_navigation_tools))
        preferences.putString(context.getString(R.string.pref_pressure_units), "in")
    }

    /**
     * Sets the layers drawn on the navigation compass, from the bottom up.
     */
    private fun showCompassLayers(vararg layerIds: String) {
        DependencyRegistry.get<MapLayerPreferenceRepo>()
            .setActiveLayerIds(NavigationToolRegistration.MAP_ID, layerIds.toList())
        // A layer in the list is still hidden if it was turned off for the compass
        val preferences = PreferencesSubsystem.getInstance(context).preferences
        layerIds.forEach {
            preferences.putBoolean(
                MapLayerPreferenceRepo.getPreferenceKey(
                    NavigationToolRegistration.MAP_ID,
                    it,
                    DefaultMapLayerDefinitions.ENABLED
                ),
                true
            )
        }
    }

    private fun hideCompassLayer(layerId: String) {
        val repo = DependencyRegistry.get<MapLayerPreferenceRepo>()
        val mapId = NavigationToolRegistration.MAP_ID
        repo.setActiveLayerIds(mapId, repo.getActiveLayerIds(mapId) - layerId)
        PreferencesSubsystem.getInstance(context).preferences.putBoolean(
            MapLayerPreferenceRepo.getPreferenceKey(
                mapId,
                layerId,
                DefaultMapLayerDefinitions.ENABLED
            ),
            false
        )
    }

    private fun setCompassLayerOpacity(layerId: String, percent: Int) {
        PreferencesSubsystem.getInstance(context).preferences.putInt(
            MapLayerPreferenceRepo.getPreferenceKey(
                NavigationToolRegistration.MAP_ID,
                layerId,
                DefaultMapLayerDefinitions.OPACITY
            ),
            percent
        )
    }

    private fun showBasemapModeledTidesAndLocation() {
        DependencyRegistry.get<MapLayerPreferenceRepo>().setActiveLayerIds(
            MapToolRegistration.MAP_ID,
            listOf(
                BaseMapTileSource.SOURCE_ID,
                TideGeoJsonSource.SOURCE_ID,
                MyLocationGeoJsonSource.SOURCE_ID
            )
        )
        val preferences = PreferencesSubsystem.getInstance(context).preferences
        listOf(TideGeoJsonSource.SHOW_MODELED_TIDES, TideGeoJsonSource.SHOW_PHASE).forEach {
            preferences.putBoolean(
                MapLayerPreferenceRepo.getPreferenceKey(
                    MapToolRegistration.MAP_ID,
                    TideGeoJsonSource.SOURCE_ID,
                    it
                ),
                true
            )
        }
    }

    /**
     * A day of readings leading up to a front: pressure drifts with the daily tide, then starts
     * falling about nine hours ago and speeds up as the front gets closer, which Trail Sense
     * forecasts as rain on the way.
     */
    private fun addApproachingFrontPressure() {
        val readingsPerHour = 4
        val hours = 24
        val frontArrivesHoursAgo = 9
        val totalDropHpa = 8f
        for (i in 0..hours * readingsPerHour) {
            val hoursAgo = hours - i / readingsPerHour.toFloat()
            val dailyTide = 0.5f * sin(2 * PI * hoursAgo / 12).toFloat()
            // Small repeatable fluctuations, so the line isn't perfectly smooth
            val noise = 0.12f * sin(hoursAgo * 7.3).toFloat() + 0.08f * sin(hoursAgo * 19.1).toFloat()
            val frontProgress = ((frontArrivesHoursAgo - hoursAgo) / frontArrivesHoursAgo).coerceIn(0f, 1f)
            val drop = totalDropHpa * frontProgress.pow(1.7f)
            TestData.addWeatherReading(
                1019f + dailyTide + noise - drop,
                Duration.ofMinutes((hoursAgo * 60).toLong() + 1),
                temperatureCelsius = 20f - 4 * frontProgress,
                humidity = 50f + 30 * frontProgress
            )
        }
    }

    private fun loadPhotoMap() {
        val file = File(context.cacheDir, "steere-hill.pdf")
        InstrumentationRegistry.getInstrumentation().context.assets.open("maps/steere-hill.pdf")
            .use { input -> file.outputStream().use { input.copyTo(it) } }
        createMap(file, "Steere Hill Farm")
    }

    /**
     * Loads the Mapsforge trail map pushed to the device (see the class docs). Returns false if it
     * isn't there.
     */
    private fun loadTrailMap(): Boolean {
        val file = File(context.cacheDir, "rhode-island.map")
        // The shell can read /data/local/tmp, but the app can't
        val output = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("cat $TRAIL_MAP_DEVICE_PATH")
        ParcelFileDescriptor.AutoCloseInputStream(output).use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        if (file.length() == 0L) {
            file.delete()
            return false
        }
        createMap(file, "Rhode Island")
        return true
    }

    private fun createMap(file: File, name: String) {
        val result = runBlocking {
            getAppService<OfflineMapService>().createMap(
                CreateOfflineMapRequest(Uri.fromFile(file), name)
            )
        }
        check(result !is Result.Err) { "Unable to load $name: $result" }
    }

    private companion object {
        @BeforeClass
        @JvmStatic
        fun requireExplicitRun() {
            val filter = InstrumentationRegistry.getArguments().getString("class").orEmpty()
            assumeTrue(
                "Screenshot tests only run when selected by name",
                filter.contains(StoreScreenshotsTest::class.java.simpleName)
            )
        }

        const val SCREENSHOT_DIRECTORY = "Pictures/TrailSenseStoreScreenshots"
        const val TRAIL_MAP_DEVICE_PATH = "/data/local/tmp/rhode-island.map"

        val BOSTON = Coordinate(42.3601, -71.0589)

        // On the Steere Hill trail in Glocester, RI (inside the photo map)
        val STEERE_HILL = Coordinate(41.8870, -71.6190)
        const val STEERE_HILL_ELEVATION = 100f

        // Places around the user, found on the trail map photo
        val NEARBY_BEACONS = listOf(
            Triple("Steere Hill", Coordinate(41.8834, -71.6180), 105f),
            Triple("Woodworth Trail", Coordinate(41.8826, -71.6213), 88f),
            Triple("Hunt Brook Crossing", Coordinate(41.8895, -71.6228), 70f),
            Triple("Stone Dam", Coordinate(41.8913, -71.6151), 95f),
        )

        // Indices into the Tuckerman Ravine GPX, which goes up from Pinkham Notch and back down
        // Heading about north on the trail, which is the direction the emulator's compass reads
        // so the user is already facing where the path goes (about half a mile from the summit)
        const val TUCKERMAN_CURRENT_INDEX = 92
        const val TUCKERMAN_SUMMIT_INDEX = 110

        // Partway around the Durfee Hill loop, the map hero's hike
        const val HIKE_CURRENT_INDEX = 30

        // Just past the far side of the Durfee Hill loop, which starts at index 21
        const val BACKTRACK_CURRENT_INDEX = 42
    }
}
