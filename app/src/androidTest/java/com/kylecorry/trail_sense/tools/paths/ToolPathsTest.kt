package com.kylecorry.trail_sense.tools.paths

import android.net.Uri
import androidx.test.uiautomator.Direction
import com.kylecorry.luna.result.Result
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.io.GpxIOService
import com.kylecorry.trail_sense.shared.io.UriPicker
import com.kylecorry.trail_sense.shared.io.UriPickerError
import com.kylecorry.trail_sense.shared.io.UriService
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.test_utils.AutomationLibrary
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.GPS_WAIT_FOR_TIMEOUT
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.backUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasDataPoint
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasTextsInOrder
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.optional
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.OPTIONAL_WAIT_FOR_TIMEOUT
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOkIfShown
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollToStart
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestData
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.clickListItemMenu
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.notifications.hasTitle
import com.kylecorry.trail_sense.test_utils.notifications.notification
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathFollow
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathRoute
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.infrastructure.PathNavigationStore
import com.kylecorry.trail_sense.tools.navigation.infrastructure.PathNavigator
import com.kylecorry.trail_sense.tools.paths.domain.FullPath
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle
import com.kylecorry.trail_sense.tools.paths.domain.PathPointColoringStyle
import com.kylecorry.trail_sense.tools.paths.domain.PathDirection
import com.kylecorry.trail_sense.tools.paths.domain.PathGPXConverter
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.infrastructure.alerts.BacktrackAlerter
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.sensors.SensorsToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant

class ToolPathsTest : ToolTestBase(Tools.PATHS, Coordinate(42.03, -71.97)) {

    // BACKTRACK (waits for the GPS)

    @Test
    fun canRecordAPathWithBacktrack() {
        hasText(R.id.paths_title, string(R.string.paths))

        // Verify it will run every minute by default
        hasText(R.id.play_bar_title, "Off - 1m")

        // Click the start button
        click(R.id.play_btn)

        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            waitFor {
                notification(BacktrackAlerter.NOTIFICATION_ID).hasTitle(R.string.backtrack)
            }
        }

        // Wait for the battery restriction warning to go away
        optional {
            hasText(string(R.string.battery_settings_limit_accuracy))
            not { hasText(string(R.string.battery_settings_limit_accuracy)) }
        }

        hasText(R.id.play_bar_title, "On - 1m")

        // Wait for the path to be created
        isVisible(com.kylecorry.andromeda.views.R.id.title, waitForTime = GPS_WAIT_FOR_TIMEOUT)

        hasText(string(R.string.temporary))

        // Stop backtrack
        click(R.id.play_btn)

        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            not { notification(BacktrackAlerter.NOTIFICATION_ID) }
        }

        // The path is kept when stopped
        not { hasText(string(R.string.temporary)) }
    }

    @Test
    fun quickActionStartsAndStopsBacktrack() {
        // TODO: Quick settings tile
        TestUtils.openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_BACKTRACK))

        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            waitFor {
                notification(BacktrackAlerter.NOTIFICATION_ID).hasTitle(R.string.backtrack)
            }
        }

        // Wait for the battery restriction warning to go away
        optional {
            hasText(string(R.string.battery_settings_limit_accuracy))
            not { hasText(string(R.string.battery_settings_limit_accuracy)) }
        }

        // Wait for the path to be created
        hasText(string(R.string.temporary), waitForTime = GPS_WAIT_FOR_TIMEOUT)
        TestUtils.closeQuickActions()
        hasText(R.id.play_bar_title, "On - 1m")

        TestUtils.openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_BACKTRACK))
        TestUtils.closeQuickActions()
        hasText(R.id.play_bar_title, "Off - 1m")

        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            not { notification(BacktrackAlerter.NOTIFICATION_ID) }
        }
    }

    // CREATING PATHS AND GROUPS THROUGH THE UI

    @Test
    fun canCreateAGroupAndAnEmptyPathAndOpenTheImportPicker() {
        hasText(R.id.paths_title, string(R.string.paths))

        click(R.id.add_btn)
        click(string(R.string.group))
        input(string(R.string.name), "Test Group")
        clickOk()

        hasText("Test Group")
        click("Test Group")
        hasText(string(R.string.no_paths))
        back(false)

        // Open import and verify the picker opens, then back out
        click(R.id.add_btn)
        click(string(R.string.import_gpx))
        optional(OPTIONAL_WAIT_FOR_TIMEOUT) { hasText(string(R.string.pick_file)) }
        back(false)
        hasText(R.id.paths_title, string(R.string.paths))

        click(R.id.add_btn)
        click(string(R.string.path), exact = true)
        input(string(R.string.name), "Empty Path")
        clickOk()
        isVisible(R.id.path_title)
        hasText("Empty Path")
        backUntil { isVisible(R.id.paths_title) }
        hasText("Empty Path")
        hasText("Test Group")
    }

    @Test
    fun canCreatePathOnMap() {
        click(R.id.add_btn)
        click(string(R.string.create_path_on_map))
        clickOkIfShown()
        hasText("Distance")
        hasText("Create path")

        // Tapping the map should add points to the path
        click(R.id.map, xPercent = 0.3f, yPercent = 0.3f)
        // The second tap is far from the first, but it is only safe once the first point was added
        waitFor { assertTrue(view(R.id.undo_btn).uiObject.isEnabled) }
        click(R.id.map, xPercent = 0.7f, yPercent = 0.3f)
        hasText(Regex("[1-9]\\d*(\\.\\d+)? (mi|ft)"))

        // Cancelling a path with points asks before discarding it
        click(toolbarButton(R.id.map_distance_title, Side.Right))
        click(string(R.string.dialog_leave))
        backUntil { isVisible(R.id.paths_title) }
    }

    // MANAGING PATHS AND GROUPS (seeded)

    @Test
    fun canRenameAndHideAPathFromTheList() {
        assumeFalse(isStagingBuild)
        var id = 0L
        relaunchTool { id = TestData.addPathFromGpx("Sprague", "paths/sprague.gpx") }
        val service = PathService.getInstance(TestUtils.context)

        clickListItemMenu("Sprague", string(R.string.rename))
        input("Sprague", "Test Path")
        clickOk()
        hasTitlesInOrder("Test Path")
        waitFor { assertEquals("Test Path", runBlocking { service.getPath(id) }!!.name) }

        click(com.kylecorry.andromeda.views.R.id.trailing_icon_btn)
        waitFor { assertFalse(runBlocking { service.getPath(id) }!!.style.visible) }
        click(com.kylecorry.andromeda.views.R.id.trailing_icon_btn)
        waitFor { assertTrue(runBlocking { service.getPath(id) }!!.style.visible) }
    }

    @Test
    fun canMergeSimplifyAndMovePaths() {
        assumeFalse(isStagingBuild)
        var groupId = 0L
        relaunchTool {
            TestData.addPathFromGpx("Durfee Loop", "paths/durfee loop.gpx")
            TestData.addPathFromGpx("Durfee Short", "paths/durfee short.gpx")
            groupId = TestData.addPathGroup("Test Group")
        }
        val service = PathService.getInstance(TestUtils.context)
        fun pathNamed(name: String) = runBlocking { service.getPaths().first() }.single { it.name == name }

        // Merge "Durfee Short" onto the end of "Durfee Loop"
        clickListItemMenu("Durfee Short", string(R.string.merge))
        click("Durfee Loop", exact = true)
        optional(OPTIONAL_WAIT_FOR_TIMEOUT) { hasText(string(R.string.merging)) }
        not { hasText("Durfee Short") }
        // 57 + 39 points. The loop is 0.9254 mi, the short loop 0.4978 mi, and the gap between them
        // 3903.8 m (2.4257 mi) by Vincenty, which is 3.8489 mi in total
        hasText("3.85 mi")
        val merged = pathNamed("Durfee Loop")
        assertEquals(96, runBlocking { service.getWaypoints(merged.id) }.size)

        // Simplify
        clickListItemMenu("Durfee Loop", string(R.string.simplify))
        hasText("will be deleted")
        click(R.id.simplify_quality)
        click("Low")
        clickOk()
        clickOk()
        waitFor { assertTrue(runBlocking { service.getWaypoints(merged.id) }.size < 96) }

        // Move to the group
        clickListItemMenu("Durfee Loop", string(R.string.move_to))
        click("Test Group")
        click(string(R.string.move))
        waitFor { assertEquals(groupId, pathNamed("Durfee Loop").parentId) }
        not { hasText("Durfee Loop") }
        click("Test Group")
        hasTitlesInOrder("Durfee Loop")
    }

    @Test
    fun canSearchRenameExportMoveAndDeleteGroups() {
        assumeFalse(isStagingBuild)
        relaunchTool {
            val groupId = TestData.addPathGroup("Test Group")
            TestData.addPathFromGpx("Durfee Short", "paths/durfee short.gpx", groupId)
            TestData.addPathGroup("Dest Group")
            TestData.addPathFromGpx("Sprague", "paths/sprague.gpx")
        }

        // Search finds paths and not the groups
        input(R.id.searchbox, "Sprague")
        hasText("Sprague")
        not { hasText("Dest Group", waitForTime = 0) }
        input(R.id.searchbox, "")
        hasText("Dest Group")

        // Rename group "Test Group" -> "Test Group 2"
        clickListItemMenu("Test Group", string(R.string.rename))
        input("Test Group", "Test Group 2")
        clickOk()
        hasText("Test Group 2")
        hasText("1 path")

        // Export current group via toolbar menu
        click(toolbarButton(R.id.paths_title, Side.Right))
        click(string(R.string.export))
        clickOk()
        optional { hasText("trail-sense-") }
        backUntil { isVisible(R.id.paths_title) }

        // Move group into Dest Group
        hasText("Test Group 2")
        clickListItemMenu("Test Group 2", string(R.string.move_to))
        click("Dest Group")
        click(string(R.string.move))
        optional(OPTIONAL_WAIT_FOR_TIMEOUT) { hasText(string(R.string.moved_to, "Dest Group")) }

        // Delete moved group (open Dest Group first)
        click("Dest Group")
        hasText("Test Group 2")
        clickListItemMenu("Test Group 2", string(R.string.delete))
        clickOk()
        not { hasText("Test Group 2") }
        back(false)
        hasText("Dest Group")
        hasText("Sprague")
    }

    // PATH DETAILS (seeded)

    @Test
    fun canChangeAPathsStyle() {
        openWithSavedPaths()
        val service = PathService.getInstance(TestUtils.context)
        fun style() = runBlocking { service.getPaths().first() }.single { it.name == "Durfee Short" }.style

        click("Durfee Short")
        hasText(R.id.path_title, "Durfee Short")

        click(R.id.path_color)
        clickOk()

        click("Dotted")
        hasOptions("Solid", "Dotted", "Arrow", "Dashed", "Square", "Diamond", "Cross")
        click("Solid")
        clickOk()
        hasText("Solid")
        waitFor { assertEquals(LineStyle.Solid, style().line) }

        changePointStyle("None", "Elevation")
        waitFor { assertEquals(PathPointColoringStyle.Altitude, style().point) }
        changePointStyle("Elevation", "None")
        waitFor { assertEquals(PathPointColoringStyle.None, style().point) }
    }

    @Test
    fun canAddAPointToAPathAndFollowIt() {
        assumeFalse(isStagingBuild)
        relaunchTool {
            TestData.addPath(
                "Test Path",
                // 100 m
                listOf(PathPoint(0, 0, Coordinate(42.03, -71.97), 100f, Instant.now()))
            )
        }

        click("Test Path")
        hasText(R.id.path_title, "Test Path")

        // A path with one point has no length or climb
        hasDataPoint("0m", string(R.string.duration))
        hasDataPoint("0 ft", string(R.string.distance))
        scrollUntil(R.id.path_scroll) {
            hasDataPoint("1", string(R.string.points))
            hasDataPoint("Easy", string(R.string.difficulty))
        }
        scrollUntil(R.id.path_scroll) {
            hasDataPoint("0 ft", string(R.string.ascent))
            hasDataPoint("0 ft", string(R.string.descent))
        }
        // 100 m is 328.08 ft
        scrollUntil(R.id.path_scroll) {
            hasDataPoint("328 ft", string(R.string.lowest_point_elevation))
            hasDataPoint("328 ft", string(R.string.highest_point_elevation))
        }
        scrollUntil(R.id.path_scroll) {
            isVisible(R.id.chart)
        }

        // Add a point away from the first so the path can be followed
        TestUtils.setLocationOverride(Coordinate(42.04, -71.97))
        scrollUntil(R.id.path_scroll) {
            click(R.id.add_point_btn)
        }

        not(waitForTime = GPS_WAIT_FOR_TIMEOUT) { hasText("Loading") }

        scrollUntil(R.id.path_scroll, direction = Direction.UP) {
            hasDataPoint("2", string(R.string.points))
            // 0.01 degrees of latitude is 1110.7 m by Vincenty
            hasDataPoint("0.69 mi", string(R.string.distance))
        }

        // Navigate
        click(string(R.string.path_direction_to_end))

        isVisible(R.id.navigation_title)
        hasText("Test Path")
        back()
        back()
        click("Test Path")

        // Path points
        click(toolbarButton(R.id.path_title, Side.Right))
        click("Points")
        hasText("Today")

        // TODO: This isn't working on the emulator
//        clickListItemMenu("Navigate")
//        isVisible(R.id.navigation_title)
//        hasText("Test Path")
//        back()
//
//        click(toolbarButton(R.id.path_title, Side.Right))
//        click("Points")
//        clickListItemMenu("Create beacon")
//        isVisible(R.id.create_beacon_title)
//        hasText("Test Path")
//        back()
//        click("Leave")
//        back()
//        isVisible(R.id.path_title)
//
//        click(toolbarButton(R.id.path_title, Side.Right))
//        click("Points")
//        clickListItemMenu("Delete")
//        clickOk()
//        back()
//
//        hasText("1")
    }

    @Test
    fun pathMenuCanReplaceElevationsSimplifyExportAndHide() {
        openWithSavedPaths()
        val service = PathService.getInstance(TestUtils.context)
        fun path() = runBlocking { service.getPaths().first() }.single { it.name == "Sprague" }

        click("Sprague")
        hasText(R.id.path_title, "Sprague")

        // Replace elevations
        click(toolbarButton(R.id.path_title, Side.Right))
        click(string(R.string.replace_elevations))
        hasText(string(R.string.replace_elevations_confirmation))
        back(false)

        // Simplify
        click(toolbarButton(R.id.path_title, Side.Right))
        click("Simplify")
        hasText("will be deleted")
        click(R.id.simplify_quality)
        hasOptions("High", "Moderate", "Low")
        click("Low")
        clickOk()
        hasText(R.id.simplify_quality, "Low")
        clickOk()
        waitFor { assertTrue(runBlocking { service.getWaypoints(path().id) }.size < 90) }

        // Export
        click(toolbarButton(R.id.path_title, Side.Right))
        click("Export")
        hasText("trail-sense-")
        backUntil {
            isVisible(R.id.path_title)
        }

        // Hide / show
        click(toolbarButton(R.id.path_title, Side.Right))
        click("Hide")
        waitFor { assertFalse(path().style.visible) }
        click(toolbarButton(R.id.path_title, Side.Right))
        click("Show")
        waitFor { assertTrue(path().style.visible) }
    }

    private fun changePointStyle(previousStyle: String, newStyle: String) {
        click(previousStyle)
        click(newStyle)
        clickOk()
        hasText(newStyle)
    }

    private fun hasOptions(vararg options: String) {
        for (option in options) {
            scrollUntil {
                hasText(option)
            }
        }
        optional(OPTIONAL_WAIT_FOR_TIMEOUT) {
            scrollToStart()
        }
    }

    // Uses real recorded hikes (androidTest/assets/paths) so the numbers shown can be checked against
    // the GPX files themselves.

    private fun openWithSavedPaths() {
        assumeFalse(isStagingBuild)
        relaunchTool { seedPaths() }
    }

    private fun seedPaths() {
        TestData.addPathFromGpx("Sprague", "paths/sprague.gpx")
        TestData.addPathFromGpx("Durfee Short", "paths/durfee short.gpx")
        TestData.addPathFromGpx("Mansfield", "paths/mount mansfield.gpx")
    }

    @Test
    fun canKeepATemporaryPathForever() {
        assumeFalse(isStagingBuild)
        relaunchTool {
            TestData.addPath(
                "Recording",
                TestData.loadGpxPoints("paths/durfee short.gpx"),
                temporary = true
            )
        }

        hasText(string(R.string.temporary))
        click("Recording")
        hasText(R.id.path_title, "Recording")

        click(toolbarButton(R.id.path_title, Side.Right))
        click(string(R.string.keep_forever))
        click(toolbarButton(R.id.path_title, Side.Right))
        not { hasText(string(R.string.keep_forever)) }
    }

    @Test
    fun canStopBacktrackFromRecordingPathDetails() {
        assumeFalse(isStagingBuild)

        click(R.id.play_btn)
        optional {
            hasText(string(R.string.battery_settings_limit_accuracy))
            not { hasText(string(R.string.battery_settings_limit_accuracy)) }
        }
        hasText(R.id.play_bar_title, "On - 1m")

        // Wait for the path to be created
        isVisible(com.kylecorry.andromeda.views.R.id.title, waitForTime = GPS_WAIT_FOR_TIMEOUT)

        click(com.kylecorry.andromeda.views.R.id.title)
        isVisible(R.id.stop_recording_btn)

        click(R.id.stop_recording_btn)
        not { isVisible(R.id.stop_recording_btn) }

        back()
        hasText(R.id.play_bar_title, "Off - 1m")
    }

    @Test
    fun listShowsEachPathWithItsLength() {
        openWithSavedPaths()

        hasText(R.id.paths_title, string(R.string.paths))
        hasTitlesAndLengths(
            "Mansfield" to "4.91 mi",
            "Durfee Short" to "0.5 mi",
            "Sprague" to "3.71 mi"
        )
    }

    @Test
    fun canSortPaths() {
        openWithSavedPaths()

        sortBy(string(R.string.most_recent), string(R.string.name))
        hasTitlesInOrder("Durfee Short", "Mansfield", "Sprague")

        sortBy(string(R.string.name), string(R.string.shortest))
        hasTitlesInOrder("Durfee Short", "Sprague", "Mansfield")

        sortBy(string(R.string.shortest), string(R.string.longest))
        hasTitlesInOrder("Mansfield", "Sprague", "Durfee Short")
    }

    @Test
    fun canSearchPaths() {
        openWithSavedPaths()

        input(R.id.searchbox, "spra")
        hasText("Sprague")
        not { hasText("Mansfield", waitForTime = 0) }
        not { hasText("Durfee Short", waitForTime = 0) }

        input(R.id.searchbox, "")
        hasText("Mansfield")
        hasText("Durfee Short")
        hasText("Sprague")
    }

    @Test
    fun pathDetailsShowTheRecordedStatistics() {
        openWithSavedPaths()

        click("Durfee Short")
        hasText(R.id.path_title, "Durfee Short")

        // Recorded from 17:20:52 to 17:36:23
        hasDataPoint("15m", string(R.string.duration))
        // 0.498 miles between the 39 recorded points
        hasDataPoint("0.5 mi", string(R.string.distance))
        scrollUntil(R.id.path_scroll) {
            hasDataPoint("39", string(R.string.points))
            hasDataPoint("Moderate", string(R.string.difficulty))
            hasDataPoint(Regex("\\d{1,2} ft"), string(R.string.ascent))
            hasDataPoint(Regex("-\\d{2} ft"), string(R.string.descent))
        }

        // The recorded elevations range from 522 ft to 590 ft
        scrollUntil(R.id.path_scroll) {
            hasDataPoint(Regex("5[2-4]\\d ft"), string(R.string.lowest_point_elevation))
        }
        hasDataPoint(Regex("5[7-9]\\d ft"), string(R.string.highest_point_elevation))
        isVisible(R.id.chart)
    }

    @Test
    fun durationAndClimbAreEstimatedFromRealRecordings() {
        openWithSavedPaths()

        click("Sprague")
        hasText(R.id.path_title, "Sprague")
        hasDataPoint("3.71 mi", string(R.string.distance))
        // The recording has no meaningful timestamps, so the duration is estimated from the pace
        hasDataPoint(Regex("1h \\d+m"), string(R.string.duration))
        scrollUntil(R.id.path_scroll) {
            hasDataPoint("90", string(R.string.points))
        }
        back()

        click("Mansfield")
        hasText(R.id.path_title, "Mansfield")
        hasDataPoint("4.91 mi", string(R.string.distance))
        // The raw recording climbs 2655 ft, the app smooths out elevation noise
        scrollUntil(R.id.path_scroll) {
            hasDataPoint("175", string(R.string.points))
            hasDataPoint(Regex("25\\d\\d ft"), string(R.string.ascent))
            hasDataPoint(Regex("-25\\d\\d ft"), string(R.string.descent))
        }
    }

    @Test
    fun canFollowAPathInEitherDirection() {
        openWithSavedPaths()

        click("Durfee Short")
        isVisible(R.id.to_start_btn)
        click(string(R.string.path_direction_to_end))

        isVisible(R.id.navigation_title)
        assertEquals(PathFollow.Direction(PathDirection.ToEnd), PathNavigationStore().load()!!.follow)
        hasText(R.id.navigation_sheet_title, "Durfee Short")
        isVisible(R.id.navigation_progress)
        // The route's distance is covered by PathRouteBuilderTest
        hasText(R.id.navigation_primary_stat, Regex("\\d+(\\.\\d+)? mi"), childId = R.id.stat_value)
        hasText(R.id.navigation_secondary_stat, Regex("(\\d+h)?\\s?(\\d+m)?\\s?(\\d+s)?"), childId = R.id.stat_value)

        // Following a different direction replaces the route
        backUntil { isVisible(R.id.path_scroll) }
        click(string(R.string.path_direction_to_start))

        isVisible(R.id.navigation_title)
        assertEquals(PathFollow.Direction(PathDirection.ToStart), PathNavigationStore().load()!!.follow)
    }

    @Test
    fun aPathWithNoLengthCannotBeFollowed() {
        assumeFalse(isStagingBuild)
        relaunchTool {
            TestData.addPath("Single Point", listOf(PathPoint(0, 0, Coordinate(42.0, -72.0), 0f)))
        }

        click("Single Point")

        isVisible(R.id.path_scroll)
        isNotVisible(R.id.follow_options)
        assertNull(PathNavigationStore().load())
    }

    @Test
    fun canNavigateToAWaypointAlongThePathOrDirectly() {
        openWithSavedPaths()

        click("Durfee Short")
        click(toolbarButton(R.id.path_title, Side.Right))
        click("Points")
        click(R.id.waypoint_menu_btn)
        click(string(R.string.path_navigation_follow_to_point))

        isVisible(R.id.navigation_title)
        assertTrue(PathNavigationStore().load()!!.follow is PathFollow.ToPoint)

        backUntil { isVisible(R.id.path_scroll) }
        click(toolbarButton(R.id.path_title, Side.Right))
        click("Points")
        click(R.id.waypoint_menu_btn)
        click(string(R.string.path_navigation_direct))

        val navigator = Navigator.getInstance(TestUtils.context)
        waitFor { assertTrue(runBlocking { navigator.getDestination() } is Destination.Beacon) }
        assertNull(PathNavigationStore().load())
    }

    @Test
    fun canDeleteAPath() {
        openWithSavedPaths()

        hasText("Durfee Short")
        clickListItemMenu("Durfee Short", string(R.string.delete))
        clickOk()

        not { hasText("Durfee Short") }
        hasText("Mansfield")
        hasText("Sprague")
    }

    private fun sortBy(current: String, new: String) {
        click(toolbarButton(R.id.paths_title, Side.Right))
        click(string(R.string.sort_by, current))
        click(new)
        clickOk()
    }

    private fun hasTitlesInOrder(vararg titles: String) {
        hasTextsInOrder(com.kylecorry.andromeda.views.R.id.title, titles.toList())
    }

    private fun hasTitlesAndLengths(vararg paths: Pair<String, String>) {
        hasTitlesInOrder(*paths.map { it.first }.toTypedArray())
        hasTextsInOrder(
            com.kylecorry.andromeda.views.R.id.description,
            paths.map { it.second }
        )
    }

    // Exports saved paths to GPX and imports the result, to verify nothing is lost in the file.

    private val uri = Uri.parse("content://test/paths.gpx")
    private val files = InMemoryUriService()
    private val service = GpxIOService(InMemoryUriPicker(uri), files)

    @Test
    fun exportedPathsCanBeImportedWithoutLosingData() = runBlocking {
        assumeFalse(isStagingBuild)
        val pathService = PathService.getInstance(TestUtils.context)
        val groupId = TestData.addPathGroup("Hikes")
        val pathId = TestData.addPathFromGpx("Durfee", "paths/durfee short.gpx", groupId)
        val original = pathService.getWaypoints(pathId)
        val fullPath = FullPath(
            pathService.getPath(pathId)!!,
            original,
            pathService.getGroup(groupId)
        )

        assertTrue(service.export(PathGPXConverter().toGPX(listOf(fullPath)), "paths.gpx"))
        val imported = service.import()

        assertNotNull(imported)
        assertEquals(1, imported!!.tracks.size)
        val track = imported.tracks[0]
        assertEquals("Durfee", track.name)
        assertEquals("Hikes", track.group)
        val points = track.segments.single().points
        assertEquals(39, points.size)
        for ((expected, actual) in original.zip(points)) {
            assertEquals(expected.coordinate.latitude, actual.coordinate.latitude, 0.00001)
            assertEquals(expected.coordinate.longitude, actual.coordinate.longitude, 0.00001)
            assertEquals(expected.elevation!!, actual.elevation!!, 0.01f)
            assertEquals(expected.time, actual.time)
        }
    }

    @Test
    fun exportedFileIsGpx() = runBlocking {
        assumeFalse(isStagingBuild)
        val pathService = PathService.getInstance(TestUtils.context)
        val pathId = TestData.addPathFromGpx("Durfee", "paths/durfee short.gpx")
        val fullPath = FullPath(
            pathService.getPath(pathId)!!,
            pathService.getWaypoints(pathId)
        )

        service.export(PathGPXConverter().toGPX(listOf(fullPath)), "paths.gpx")

        val text = files.text!!
        assertTrue(text.contains("<gpx"))
        assertTrue(text.contains("creator=\"Trail Sense\""))
        assertEquals(39, Regex("<trkpt ").findAll(text).count())
    }

    @Test
    fun multiplePathsAreExportedAsSeparateTracks() = runBlocking {
        assumeFalse(isStagingBuild)
        val pathService = PathService.getInstance(TestUtils.context)
        val paths = listOf("Durfee" to "paths/durfee short.gpx", "Sprague" to "paths/sprague.gpx")
            .map { (name, asset) ->
                val id = TestData.addPathFromGpx(name, asset)
                FullPath(pathService.getPath(id)!!, pathService.getWaypoints(id))
            }

        service.export(PathGPXConverter().toGPX(paths), "paths.gpx")
        val imported = service.import()!!

        assertEquals(listOf("Durfee", "Sprague"), imported.tracks.map { it.name })
        assertEquals(listOf(39, 90), imported.tracks.map { it.segments.single().points.size })
    }

    // PATH NAVIGATION
    // These use the navigator directly, with real hikes (androidTest/assets/paths), the database,
    // and the saved navigation state. The routing logic itself is covered by PathRouteTest.
    //
    // Durfee Short is a loop (its start and end are 9 m apart) and Durfee Out is an out and back
    // path. The expected distances were calculated from the GPX files.

    // The point 198 m along the loop, there is no other part of the loop nearby

    @Test
    fun navigationIsRestoredWithItsProgress() = runBlocking {
        val points = coordinatesOf(OUT_AND_BACK)
        val route = follow(OUT_AND_BACK, reversed = false, from = points[0])
        val location = Coordinate(points[28].latitude + 0.00001, points[28].longitude)
        route.navigate(points[28])
        // The navigator saves the progress when the location changes
        UserPreferences(TestUtils.context).gps.locationOverride = location
        Tools.broadcast(SensorsToolRegistration.BROADCAST_LOCATION_CHANGED)
        waitFor { assertNotNull(PathNavigationStore().load()?.progress) }

        // A new navigator is what the app has after it is restarted
        val restored = PathNavigator(TestUtils.context)
        restored.awaitRestore()
        val destination = restored.destination.first()

        assertNotNull(destination)
        assertEquals("Durfee Out", destination!!.path.name)
        // Without the saved progress the route would start over and need to find the shortcut again
        assertEquals(663.6f, destination.route.navigate(location).remainingDistance.meters().value, 3f)
    }

    @Test
    fun navigatingToAnotherPathReplacesThePreviousOneUntilItIsStopped() = runBlocking {
        val navigator = PathNavigator(TestUtils.context)
        follow(OUT_AND_BACK, reversed = false, from = coordinatesOf(OUT_AND_BACK)[0], navigator)
        assertEquals("Durfee Out", navigator.destination.first()!!.path.name)

        follow(LOOP, reversed = false, from = coordinatesOf(LOOP)[0], navigator)

        assertEquals("Durfee Short", navigator.destination.first()!!.path.name)
        val restored = PathNavigator(TestUtils.context)
        restored.awaitRestore()
        assertEquals("Durfee Short", restored.destination.first()!!.path.name)

        navigator.cancel()

        assertNull(navigator.destination.first())
        val restoredAfterStopping = PathNavigator(TestUtils.context)
        restoredAfterStopping.awaitRestore()
        assertNull(restoredAfterStopping.destination.first())
    }

    @Test
    fun restoredNavigationUsesTheElevationSmoothingSetting() = runBlocking {
        val prefs = UserPreferences(TestUtils.context).paths
        val start = coordinatesOf(OUT_AND_BACK)[0]

        val defaultGain = follow(OUT_AND_BACK, reversed = false, from = start)
            .navigate(start).remainingElevationGain.meters().value
        PreferencesSubsystem.getInstance(TestUtils.context).preferences
            .putInt(TestUtils.context.getString(R.string.pref_elevation_smoothing), 1000)
        val restored = PathNavigator(TestUtils.context)
        restored.awaitRestore()
        val smoothedGain = restored.destination.first()!!.route
            .navigate(start).remainingElevationGain.meters().value

        assertEquals(1f, prefs.elevationSmoothing, 0.001f)
        assertTrue(smoothedGain < defaultGain)
    }

    @Test
    fun findsTheMostRecentPathWithEnoughElevationChange() = runBlocking {
        val pathService = PathService.getInstance(TestUtils.context)
        assertNull(pathService.getMostRecentPathIdWithElevationChange(50f))

        val hilly = TestData.addPathFromGpx("Hilly", "paths/presidential range.gpx")
        TestData.addPathFromGpx("Flat", "paths/durfee short.gpx")

        assertEquals(hilly, pathService.getMostRecentPathIdWithElevationChange(50f))
        assertNull(pathService.getMostRecentPathIdWithElevationChange(5000f))
    }

    /**
     * Saves the path and starts following it from the location.
     */
    private suspend fun follow(
        asset: String,
        reversed: Boolean,
        from: Coordinate,
        navigator: PathNavigator = PathNavigator(TestUtils.context)
    ): PathRoute {
        assumeFalse(isStagingBuild)
        UserPreferences(TestUtils.context).gps.locationOverride = from
        val name = if (asset == LOOP) "Durfee Short" else "Durfee Out"
        val pathService = PathService.getInstance(TestUtils.context)
        val id = TestData.addPathFromGpx(name, asset)
        val direction = if (reversed) PathDirection.ToStart else PathDirection.ToEnd
        navigator.navigate(pathService.getPath(id)!!, PathFollow.Direction(direction))
        navigator.awaitRestore()
        return navigator.destination.first()!!.route
    }

    private fun PathRoute.navigate(location: Coordinate) = navigate(location, null)

    private fun coordinatesOf(asset: String): List<Coordinate> {
        return TestData.loadGpxPoints(asset).map { it.coordinate }
    }

    private class InMemoryUriPicker(private val uri: Uri) : UriPicker {
        override suspend fun open(
            types: List<String>,
            requirePersistentAccess: Boolean
        ): Result<Uri, UriPickerError> = Result.Ok(uri)

        override suspend fun create(filename: String, type: String): Uri = uri
    }

    private class InMemoryUriService : UriService {
        var text: String? = null

        override suspend fun write(uri: Uri, data: String): Boolean {
            text = data
            return true
        }

        override suspend fun outputStream(uri: Uri): OutputStream = ByteArrayOutputStream()

        override suspend fun read(uri: Uri): String? = text

        override suspend fun inputStream(uri: Uri): InputStream? {
            return text?.let { ByteArrayInputStream(it.toByteArray()) }
        }
    }

    companion object {
        // A loop (the start and end are 9 m apart)
        private const val LOOP = "paths/durfee short.gpx"

        // Ends 543 m from where it started
        private const val OUT_AND_BACK = "paths/durfee loop.gpx"
    }
}
