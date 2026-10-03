package com.kylecorry.trail_sense.tools.paths

import android.net.Uri
import androidx.test.uiautomator.Direction
import com.kylecorry.luna.result.Result
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.io.GpxIOService
import com.kylecorry.trail_sense.shared.io.UriPicker
import com.kylecorry.trail_sense.shared.io.UriPickerError
import com.kylecorry.trail_sense.shared.io.UriService
import com.kylecorry.trail_sense.test_utils.AutomationLibrary
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.GPS_WAIT_FOR_TIMEOUT
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.backUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasDataPoint
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasTextsInOrder
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.optional
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
import com.kylecorry.trail_sense.tools.paths.domain.FullPath
import com.kylecorry.trail_sense.tools.paths.domain.PathGPXConverter
import com.kylecorry.trail_sense.tools.paths.infrastructure.alerts.BacktrackAlerter
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

class ToolPathsTest : ToolTestBase(Tools.PATHS, Coordinate(42.03, -71.97)) {
    @Test
    fun verifyBasicFunctionality() {
        hasText(R.id.paths_title, string(R.string.paths))

        canUseBacktrack()
        canRenamePath()
        canViewPathDetails()
        addPathGroup()
        importPath()
        createEmptyPath()
        groupOperations()
        pathOperations()
        searchPath()
        changeSort()
        // TODO: Quick settings tile
        verifyQuickAction()
    }

    private fun addPathGroup() {
        click(R.id.add_btn)
        click(string(R.string.group))
        input(string(R.string.name), "Test Group")
        clickOk()

        hasText("Test Group")
        click("Test Group")
        hasText(string(R.string.no_paths))
        back(false)
    }

    private fun importPath() {
        // Open import and verify the picker opens, then back out
        click(R.id.add_btn)
        click(string(R.string.import_gpx))
        optional { hasText(string(R.string.pick_file)) }
        back(false)
        hasText(R.id.paths_title, string(R.string.paths))
    }

    private fun createEmptyPath() {
        click(R.id.add_btn)
        click(string(R.string.path), exact = true)
        input(string(R.string.name), "Empty Path")
        clickOk()
        isVisible(R.id.path_title)
        hasText("Empty Path")
        backUntil { isVisible(R.id.paths_title) }
    }

    private fun groupOperations() {
        // Ensure there is a second group to move into
        click(R.id.add_btn)
        click(string(R.string.group), exact = true)
        input(string(R.string.name), "Dest Group")
        clickOk()
        hasText("Dest Group")

        // Rename group "Test Group" -> "Test Group 2"
        hasText("Test Group")
        clickListItemMenu(string(R.string.rename), index = 2)
        input("Test Group", "Test Group 2")
        clickOk()
        hasText("Test Group 2")

        // Export current group via toolbar menu
        click(toolbarButton(R.id.paths_title, Side.Right))
        click(string(R.string.export))
        clickOk()
        optional { hasText("trail-sense-") }
        backUntil { isVisible(R.id.paths_title) }

        // Move group into Dest Group
        hasText("Test Group 2")
        clickListItemMenu(string(R.string.move_to), index = 2)
        click("Dest Group")
        click(string(R.string.move))
        optional { hasText(string(R.string.moved_to, "Dest Group")) }

        // Delete moved group (open Dest Group first)
        click("Dest Group")
        hasText("Test Group 2")
        clickListItemMenu(string(R.string.delete))
        clickOk()
        not { hasText("Test Group 2") }
        back(false)
    }

    private fun pathOperations() {
        // Create another path
        click(R.id.add_btn)
        click(string(R.string.path), exact = true)
        input(string(R.string.name), "Second Path")
        clickOk()
        hasText("Second Path")
        backUntil { isVisible(R.id.paths_title) }

        // Rename
        clickListItemMenu(string(R.string.rename))
        input("Second Path", "Empty Path 2")
        clickOk()
        hasText("Empty Path 2")

        // Hide / Show
        click(com.kylecorry.andromeda.views.R.id.trailing_icon_btn)
        click(com.kylecorry.andromeda.views.R.id.trailing_icon_btn)

        // Export
        clickListItemMenu(string(R.string.export))
        optional { hasText("trail-sense-") }
        backUntil { isVisible(R.id.paths_title) }

        // Merge "Empty Path 2" into "Empty Path"
        clickListItemMenu(string(R.string.merge))
        click("Empty Path", exact = true)
        optional { hasText(string(R.string.merging)) }
        not { hasText("Empty Path 2") }

        // Simplify path
        clickListItemMenu(string(R.string.simplify))
        click("High")
        clickOk()

        // Move Empty Path to Dest Group
        clickListItemMenu(string(R.string.move_to))
        click("Dest Group")
        click(string(R.string.move))

        // Delete path
        clickListItemMenu(string(R.string.delete), index = 1)
        clickOk()
        not { hasText("Empty Path 2") }
    }

    private fun searchPath() {
        input(R.id.searchbox, "Path")
        hasText("Empty Path")
        not { hasText("Dest Group") }
        input(R.id.searchbox, "")
        hasText("Dest Group")
    }

    private fun changeSort() {
        click(toolbarButton(R.id.paths_title, Side.Right))
        click(string(R.string.sort_by, string(R.string.most_recent)))
        click(string(R.string.name))
        clickOk()
        click(toolbarButton(R.id.paths_title, Side.Right))
        hasText(string(R.string.sort_by, string(R.string.name)))
        back(false)
    }

    private fun canViewPathDetails() {
        // Open the path
        click(com.kylecorry.andromeda.views.R.id.title)

        // Wait for the path to open
        hasText(R.id.path_title, "Test Path")

        // Settings
        click(R.id.path_color)
        clickOk()

        click("Dotted")
        hasOptions("Solid", "Dotted", "Arrow", "Dashed", "Square", "Diamond", "Cross")
        click("Solid")
        clickOk()
        hasText("Solid")

        changePointStyle("None", "Cell signal")
        changePointStyle("Cell signal", "Elevation")
        changePointStyle("Elevation", "Time")
        changePointStyle("Time", "Slope")
        changePointStyle("Slope", "None")

        // Stats
        hasText("0m")
        hasText("Duration")

        hasText("1")
        hasText("Points")

        hasText("0 ft")
        hasText("Distance")

        hasText("Easy")
        hasText("Difficulty")

        scrollUntil(R.id.path_scroll) {
            hasText("0 ft")
            hasText("Ascent")
        }

        hasText("0 ft")
        hasText("Descent")

        scrollUntil(R.id.path_scroll) {
            hasText(Regex("\\d+ ft"))
            hasText("Lowest point")
        }

        hasText(Regex("\\d+ ft"))
        hasText("Highest point")

        scrollUntil(R.id.path_scroll) {
            isVisible(R.id.chart)
        }

        // Add a point
        scrollUntil(R.id.path_scroll) {
            click(R.id.add_point_btn)
        }

        not(waitForTime = GPS_WAIT_FOR_TIMEOUT) { hasText("Loading") }

        scrollUntil(R.id.path_scroll, direction = Direction.UP) {
            hasText("2")
        }

        // Navigate
        scrollUntil(R.id.path_scroll) {
            click("Navigate")
        }

        click(string(R.string.path_navigation_follow))
        clickOk()
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
        back()

    // Replace elevations
    click(toolbarButton(R.id.path_title, Side.Right))
    click(string(R.string.replace_elevations))
    hasText(string(R.string.replace_elevations_confirmation))
    back(false)

        // Simplify
        click(toolbarButton(R.id.path_title, Side.Right))
        click("Simplify")
        hasOptions("High", "Moderate", "Low")
        click("High")
        clickOk()

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
        click(toolbarButton(R.id.path_title, Side.Right))
        click("Show")

        // Keep forever
        click(toolbarButton(R.id.path_title, Side.Right))
        click("Keep forever")
        click(toolbarButton(R.id.path_title, Side.Right))
        not { hasText("Keep forever") }

        backUntil { isVisible(R.id.paths_title) }
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
        optional {
            scrollToStart()
        }
    }

    private fun canRenamePath() {
        clickListItemMenu(string(R.string.rename))
        input(string(R.string.name), "Test Path")
        clickOk()
        hasText(com.kylecorry.andromeda.views.R.id.title, "Test Path")
    }

    private fun canUseBacktrack() {
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

        // Stop backtrack
        click(R.id.play_btn)

        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            not { notification(BacktrackAlerter.NOTIFICATION_ID) }
        }
    }

    private fun verifyQuickAction() {
        TestUtils.openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_BACKTRACK))

        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            waitFor {
                notification(BacktrackAlerter.NOTIFICATION_ID).hasTitle(R.string.backtrack)
            }
        }

        // Wait for the path to be created
        isVisible(
            com.kylecorry.andromeda.views.R.id.title,
            index = 1,
            waitForTime = GPS_WAIT_FOR_TIMEOUT
        )

        click(quickAction(Tools.QUICK_ACTION_BACKTRACK))

        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            not { notification(BacktrackAlerter.NOTIFICATION_ID) }
        }

        TestUtils.closeQuickActions()
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
        hasDataPoint("39", string(R.string.points))
        hasDataPoint("Easy", string(R.string.difficulty))
        hasDataPoint(Regex("\\d{1,2} ft"), string(R.string.ascent))
        hasDataPoint(Regex("-\\d{2} ft"), string(R.string.descent))

        // The recorded elevations range from 522 ft to 590 ft
        scrollUntil(R.id.path_scroll) {
            hasDataPoint(Regex("5[2-4]\\d ft"), string(R.string.lowest_point_elevation))
        }
        hasDataPoint(Regex("5[7-9]\\d ft"), string(R.string.highest_point_elevation))
        isVisible(R.id.chart)
    }

    @Test
    fun difficultyReflectsLengthAndClimb() {
        openWithSavedPaths()

        click("Sprague")
        hasText(R.id.path_title, "Sprague")
        hasDataPoint("3.71 mi", string(R.string.distance))
        hasDataPoint("90", string(R.string.points))
        // The recording has no meaningful timestamps, so the duration is estimated from the pace
        hasDataPoint(Regex("1h \\d+m"), string(R.string.duration))
        hasDataPoint("Moderate", string(R.string.difficulty))
        back()

        click("Mansfield")
        hasText(R.id.path_title, "Mansfield")
        hasDataPoint("4.91 mi", string(R.string.distance))
        hasDataPoint("175", string(R.string.points))
        hasDataPoint("Hard", string(R.string.difficulty))
        // The raw recording climbs 2655 ft, the app smooths out elevation noise
        hasDataPoint(Regex("25\\d\\d ft"), string(R.string.ascent))
        hasDataPoint(Regex("-25\\d\\d ft"), string(R.string.descent))
    }

    @Test
    fun canFollowAPathFromTheCurrentLocation() {
        openWithSavedPaths()

        click("Durfee Short")
        scrollUntil(R.id.path_scroll) {
            click(string(R.string.navigate))
        }
        click(string(R.string.path_navigation_follow))
        clickOk()

        isVisible(R.id.navigation_title)
        hasText(R.id.navigation_sheet_title, "Durfee Short")
        // The path is 12.99 miles from the current location
        hasText(R.id.navigation_distance, "13 mi")
        hasText(R.id.navigation_eta, Regex("(\\d+h)?\\s?(\\d+m)?\\s?(\\d+s)?"))
    }

    @Test
    fun canDeleteAPath() {
        openWithSavedPaths()

        hasText("Durfee Short")
        // The list is ordered by most recent: Mansfield, Durfee Short, Sprague
        clickListItemMenu(string(R.string.delete), index = 1)
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
}
