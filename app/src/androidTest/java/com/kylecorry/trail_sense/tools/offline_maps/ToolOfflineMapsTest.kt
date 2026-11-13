package com.kylecorry.trail_sense.tools.offline_maps

import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.views.Toolbar
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.backUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasTextsInOrder
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.longClick
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.optional
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOkIfShown
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestData
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.clickListItemMenu
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.tools.map.ui.MapView
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapFile
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.projections.MapProjectionType
import com.kylecorry.trail_sense.tools.offline_maps.domain.trail_maps.TrailMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.persistence.MapRepo
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Test
import java.time.Instant
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class ToolOfflineMapsTest : ToolTestBase(Tools.OFFLINE_MAPS, location) {

    // PHOTO MAPS (created through the UI)

    @Test
    fun canCreateAPhotoMapFromTheCamera() {
        // Disclaimer
        clickOk()

        hasText(R.id.map_list_title, string(R.string.offline_maps))

        // No maps by default
        hasText(string(R.string.no_maps))

        canCreateMapFromCamera()
        hasTitlesInOrder("Test Map")
    }

    @Test
    fun canCreateABlankMapInAGroup() {
        clickOk()

        click(R.id.add_btn)
        click(string(R.string.group))

        input("Name", "Test Group", index = 1)
        clickOk()
        hasText("Test Group")
        hasText("0 maps")
        click("Test Group")
        hasText(string(R.string.no_maps))

        // Create a map in the group
        canCreateBlankMap("Blank Map")

        hasTitlesInOrder("Blank Map")
        back()
        hasTitlesInOrder("Test Group")
        hasText("1 map")
    }

    // PHOTO MAPS (seeded)

    @Test
    fun canRenameSearchAndDeleteAMapGroup() {
        val groupId = openWith {
            val groupId = TestData.addMapGroup("Test Group")
            TestData.addPhotoMap("Blank Map", location, groupId)
            TestData.addPhotoMap("Test Map", location)
            groupId
        }
        hasText("1 map")

        clickListItemMenu("Test Group", string(R.string.rename))
        input("Test Group", "Test Group 2")
        clickOk()
        hasText("Test Group 2")
        hasText("1 map")
        assertEquals("Test Group 2", runBlocking { MapRepo.getInstance(TestUtils.context).getMapGroup(groupId) }?.name)

        // Searching finds the maps inside of groups
        input(R.id.searchbox, "Blank")
        hasTitlesInOrder("Blank Map")
        input(R.id.searchbox, "")
        hasText("Test Group 2")

        // Deleting the group deletes the maps in it
        clickListItemMenu("Test Group 2", string(R.string.delete))
        clickOk()
        waitFor { hasTitlesInOrder("Test Map") }
        assertEquals(1, runBlocking { MapRepo.getInstance(TestUtils.context).getPhotoMaps().size })
    }

    @Test
    fun canRenameHideMoveAndDeleteAMap() {
        val ids = openWith {
            val mapId = TestData.addPhotoMap("Test Map", location)
            val groupId = TestData.addMapGroup("Test Group")
            mapId to groupId
        }
        val repo = MapRepo.getInstance(TestUtils.context)

        click(com.kylecorry.andromeda.views.R.id.trailing_icon_btn)
        waitFor { assertFalse(runBlocking { repo.getPhotoMap(ids.first) }!!.visible) }
        click(com.kylecorry.andromeda.views.R.id.trailing_icon_btn)
        waitFor { assertTrue(runBlocking { repo.getPhotoMap(ids.first) }!!.visible) }

        clickListItemMenu("Test Map", string(R.string.rename))
        input("Test Map", "Test Map 2")
        clickOk()
        hasText("Test Map 2")
        assertEquals("Test Map 2", runBlocking { repo.getPhotoMap(ids.first) }!!.name)

        clickListItemMenu("Test Map 2", string(R.string.move_to))
        click("Test Group")
        click(string(R.string.move))
        hasText("1 map")
        not { hasText("Test Map 2") }
        assertEquals(ids.second, runBlocking { repo.getPhotoMap(ids.first) }!!.parentId)
        click("Test Group")
        hasTitlesInOrder("Test Map 2")

        clickListItemMenu("Test Map 2", string(R.string.delete))
        clickOk()
        hasText(string(R.string.no_maps))
        assertTrue(runBlocking { repo.getPhotoMaps() }.isEmpty())
    }

    @Test
    fun canChangeTheResolutionExportAndPrintAMap() {
        openWith { TestData.addPhotoMap("Blank Map", location) }

        clickListItemMenu("Blank Map", string(R.string.change_resolution))
        click("Moderate")
        clickOk()
        waitFor { hasText("Blank Map") }

        clickListItemMenu("Blank Map", string(R.string.export))
        // Pressing back is needed sometimes to close the drive selector
        backUntil {
            hasText("blank-map.pdf")
        }
        backUntil {
            isVisible(R.id.map_list_title)
        }

        clickListItemMenu("Blank Map", string(R.string.print))
        hasText("Copies")
        backUntil {
            isVisible(R.id.map_list_title)
        }
    }

    // VIEWING A PHOTO MAP (seeded, the map is centered on the current location)

    private fun openPhotoMap(): Long {
        val id = openWith { TestData.addPhotoMap("Test Map", location) }
        click("Test Map")
        hasText(R.id.map_title, "Test Map")
        return id
    }

    @Test
    fun longPressingAPhotoMapShowsTheLocationAndCanCreateABeacon() {
        openPhotoMap()

        longClick(R.id.map)
        val shown = parseCoordinate(textOf(R.id.toolbar_title))
        // The press is at the center of the map, which is centered on the current location
        assertEquals(location.latitude, shown.latitude, 0.0003)
        assertEquals(location.longitude, shown.longitude, 0.0003)
        hasText(Regex("Elevation: -?\\d+(\\.\\d+)?\\s*(ft|m)"))
        // Within a few pixels of the current location
        val expectedFeet = distanceMeters(location, shown) * 3.28084
        val distance = Regex("(\\d+) ft")
            .find(textOf(R.id.location_data_distance))!!.groupValues[1].toInt()
        assertEquals(expectedFeet, distance.toDouble(), 3.0)
        hasText(R.id.location_data_direction, Regex("-?\\d+°\\s+\\w+"))
        hasText(R.id.location_data_elevation_diff, Regex("[+-]?\\d+(\\.\\d+)?\\s*(ft|m)"))
        hasText("Beacon")
        hasText("Navigate")
        hasText("Distance")

        click("Beacon")
        hasText("Create beacon")
        hasText(Regex(".*-?\\d+\\.\\d+°,\\s+-?\\d+\\.\\d+°.*"))
        back()
        click("Leave")
    }

    @Test
    fun longPressingAPhotoMapCanNavigateAndMeasure() {
        openPhotoMap()

        longClick(R.id.map)
        val shown = parseCoordinate(textOf(R.id.toolbar_title))
        click("Navigate")
        // The destination is named after the map
        hasText("Test Map", index = 1)
        val expectedFeet = distanceMeters(location, shown) * 3.28084
        val stat = textOf(R.id.navigation_primary_stat)
        assertEquals(expectedFeet, Regex("(\\d+) ft").find(stat)!!.groupValues[1].toDouble(), 3.0)
        click(R.id.navigation_cancel)

        longClick(R.id.map)
        click("Distance")
        hasText("Distance")
        hasText("Create path")
        // The measurement starts from the current location
        waitFor {
            assertEquals(expectedFeet, measuredFeet(), 3.0)
        }
        click(toolbarButton(R.id.map_distance_title, Side.Right))
    }

    @Test
    fun photoMapControlsZoomLockAndTrace() {
        openPhotoMap()

        // Zooming out is limited to the whole map, so zoom in first
        val start = resolution()
        click(R.id.zoom_in_btn)
        waitFor { assertEquals(start / 2, resolution(), start * 0.001f) }
        click(R.id.zoom_out_btn)
        waitFor { assertEquals(start, resolution(), start * 0.001f) }
        click(toolbarButton(R.id.map_title, Side.Left))

        isNotChecked(R.id.lock_btn)
        click(R.id.lock_btn)
        isChecked(R.id.lock_btn)
        click(R.id.lock_btn)
        isChecked(R.id.lock_btn)
        click(R.id.lock_btn)
        isNotChecked(R.id.lock_btn)
        click(toolbarButton(R.id.map_title, Side.Left))

        // Trace
        click(toolbarButton(R.id.map_title, Side.Right))
        click("Trace")
        clickOk()
        not { isVisible(R.id.zoom_in_btn) }
        not { isVisible(R.id.zoom_out_btn) }
        // Bottom nav does nothing
        click(R.id.bottom_navigation)
        click(R.id.lock_btn)
        isVisible(R.id.zoom_in_btn)
        isVisible(R.id.zoom_out_btn)
    }

    @Test
    fun mapMenuCanCalibrateRenameAndChangeProjection() {
        val id = openPhotoMap()

        // Calibrate
        click(toolbarButton(R.id.map_title, Side.Right))
        click("Calibrate")
        clickOkIfShown()
        hasText("Point 1 of 2")
        waitFor {
            // The click can land before the page settles, so repeat it until the next step shows
            try {
                hasText("Finish calibration", waitForTime = 1)
            } catch (e: Throwable) {
                click("Next", waitForTime = 1)
                throw e
            }
        }
        click("Finish calibration")
        isVisible(R.id.map)

        // User guide
        click(toolbarButton(R.id.map_title, Side.Right))
        click("User Guide")
        hasText("Offline Maps")
        back()
        isVisible(R.id.map)

        // Rename
        click(toolbarButton(R.id.map_title, Side.Right))
        click("Rename")
        input("Test Map", "Test Map 2")
        clickOk()
        hasText(R.id.map_title, "Test Map 2")

        // Change projection
        click(toolbarButton(R.id.map_title, Side.Right))
        click("Change projection")
        click("Equidistant")
        clickOk()

        val repo = MapRepo.getInstance(TestUtils.context)
        waitFor {
            val map = runBlocking { repo.getPhotoMap(id) }!!
            assertEquals("Test Map 2", map.name)
            assertEquals(MapProjectionType.CylindricalEquidistant, map.georeference.projectionType)
        }
    }

    @Test
    fun mapMenuCanMeasureCreateAPathAndShowLayers() {
        openPhotoMap()

        // Measure
        click(toolbarButton(R.id.map_title, Side.Right))
        click("Measure")
        hasText("Distance")
        // No points have been added yet
        hasText(R.id.map_distance_title, "0 ft")
        hasText("Create path")
        click(toolbarButton(R.id.map_distance_title, Side.Right))

        // Create path
        click(toolbarButton(R.id.map_title, Side.Right))
        click("Create path")
        hasText("Distance")
        hasText(R.id.map_distance_title, "0 ft")
        hasText("Create path")
        click(toolbarButton(R.id.map_distance_title, Side.Right))

        // Layers
        click(toolbarButton(R.id.map_title, Side.Right))
        click("Layers")
        scrollUntil { hasText("Location") }
        scrollUntil { hasText("Beacons") }
        scrollUntil { hasText("Paths") }
        scrollUntil { hasText("Tides") }
        scrollUntil { hasText("Navigation") }
        scrollUntil { hasText("Photo maps") }
        click(toolbarButton(R.id.title, Side.Right))
    }

    @Test
    fun mapMenuCanExportPrintAndDeleteAMap() {
        openPhotoMap()

        // Export
        click(toolbarButton(R.id.map_title, Side.Right))
        click("Export")
        // Pressing back is needed sometimes to close the drive selector
        backUntil {
            hasText("test-map.pdf")
        }
        backUntil {
            isVisible(R.id.map)
        }

        // Print
        click(toolbarButton(R.id.map_title, Side.Right))
        click("Print")
        hasText("Copies")
        backUntil {
            isVisible(R.id.map)
        }

        // Delete
        click(toolbarButton(R.id.map_title, Side.Right))
        click(string(R.string.delete))
        clickOk()

        isVisible(R.id.map_list_title)
        hasText(string(R.string.no_maps))
        assertTrue(runBlocking { MapRepo.getInstance(TestUtils.context).getPhotoMaps() }.isEmpty())
    }

    // MAPSFORGE MAPS

    @Test
    fun canOpenTheFilePickerAndCreateAnOfflineMapGroup() {
        // TODO: To run on staging builds another solution to populating maps will need to be used
        assumeFalse(isStagingBuild)

        // Offline maps disclaimer
        clickOkIfShown()

        hasText(R.id.map_list_title, string(R.string.offline_maps))
        hasText(string(R.string.no_maps))

        canOpenOfflineMapFilePicker()
        canCreateOfflineMapGroup()
    }

    @Test
    fun canSearchOfflineMaps() {
        openWith {
            seedOfflineMap("Second Map")
            seedOfflineMap("Test Map")
            TestData.addMapGroup("Test Group")
        }

        input(R.id.searchbox, "Second")
        hasTitlesInOrder("Second Map")
        input(R.id.searchbox, "Test")
        not { hasText("Second Map") }
        hasText("Test Map")
        input(R.id.searchbox, "")
        hasText("Second Map")
        hasText("Test Map")
        hasText("Test Group")
        hasText(R.id.map_list_title, string(R.string.offline_maps))
    }

    @Test
    fun canToggleViewAndExportAnOfflineMap() {
        val id = openWith { seedOfflineMap("Test Map") }
        val repo = MapRepo.getInstance(TestUtils.context)

        click(com.kylecorry.andromeda.views.R.id.trailing_icon_btn)
        waitFor { assertFalse(runBlocking { repo.getTrailMap(id) }!!.visible) }
        click(com.kylecorry.andromeda.views.R.id.trailing_icon_btn)
        waitFor { assertTrue(runBlocking { repo.getTrailMap(id) }!!.visible) }

        click("Test Map")
        hasText("Test Map")
        click(R.id.zoom_in_btn)
        click(R.id.zoom_out_btn)
        back()

        clickListItemMenu("Test Map", string(R.string.export))
        backUntil {
            hasText("test-map.map")
        }
        backUntil {
            isVisible(R.id.map_list_title)
        }

        click("Test Map")
        waitFor {
            isVisible(R.id.zoom_in_btn)
        }
        click(toolbarButton(R.id.title, Side.Right))
        click(string(R.string.export))
        backUntil {
            hasText("test-map.map")
        }
        backUntil {
            isVisible(R.id.map)
        }
        back()
    }

    @Test
    fun canRenameEditTheAttributionAndMoveAnOfflineMap() {
        val ids = openWith {
            val mapId = seedOfflineMap("Test Map")
            mapId to TestData.addMapGroup("Test Group")
        }
        val repo = MapRepo.getInstance(TestUtils.context)

        clickListItemMenu("Test Map", string(R.string.rename))
        input("Test Map", "Test Map 2")
        clickOk()
        hasText("Test Map 2")
        assertEquals("Test Map 2", runBlocking { repo.getTrailMap(ids.first) }!!.name)

        clickListItemMenu("Test Map 2", string(R.string.attribution))
        input(string(R.string.attribution), "Test Attribution", isHint = true)
        clickOk()
        waitFor {
            assertEquals("Test Attribution", runBlocking { repo.getTrailMap(ids.first) }!!.attribution)
        }

        clickListItemMenu("Test Map 2", string(R.string.move_to))
        click("Test Group")
        click(string(R.string.move))
        hasText("1 map")
        not { hasText("Test Map 2") }
        assertEquals(ids.second, runBlocking { repo.getTrailMap(ids.first) }!!.parentId)
        click("Test Group")
        hasTitlesInOrder("Test Map 2")
    }

    @Test
    fun canRenameAndDeleteAnOfflineMapGroupAndMap() {
        openWith {
            val groupId = TestData.addMapGroup("Test Group")
            seedOfflineMap("Test Map", groupId)
            seedOfflineMap("Second Map")
        }

        clickListItemMenu("Test Group", string(R.string.rename))
        input("Test Group", "Test Group 2")
        clickOk()
        hasText("Test Group 2")
        hasText("1 map")

        // Deleting the group deletes the maps in it
        clickListItemMenu("Test Group 2", string(R.string.delete))
        clickOk()
        not { hasText("Test Group 2") }
        waitFor { hasTitlesInOrder("Second Map") }

        clickListItemMenu("Second Map", string(R.string.delete))
        clickOk()
        hasText(string(R.string.no_maps))
    }

    private fun <T> openWith(seed: () -> T): T {
        assumeFalse(isStagingBuild)
        var seeded: T? = null
        relaunchTool { seeded = seed() }
        clickOkIfShown()
        @Suppress("UNCHECKED_CAST")
        return seeded as T
    }

    private fun hasTitlesInOrder(vararg titles: String) {
        hasTextsInOrder(com.kylecorry.andromeda.views.R.id.title, titles.toList())
    }

    private fun canOpenOfflineMapFilePicker() {
        click(R.id.add_btn)
        click(string(R.string.map_file))

        // The file picker is opened
        isNotVisible(R.id.add_btn)

        backUntil { isVisible(R.id.map_list_title) }
    }

    private fun canCreateOfflineMapGroup() {
        click(R.id.add_btn)
        click(string(R.string.group))

        input(string(R.string.name), "Test Group", index = 1)
        clickOk()
        hasText("Test Group")
        hasText("0 maps")
    }

    private fun seedOfflineMap(name: String, groupId: Long? = null) = runBlocking {
        val files = getAppService<FileSubsystem>()
        files.getDirectory("offline_maps", true)
        val filename = name.lowercase().replace(" ", "-")
        val path = "offline_maps/$filename.map"
        files.get(path, true).writeBytes(ByteArray(0))

        MapRepo.getInstance(TestUtils.context).add(
            TrailMap(
                0,
                name,
                listOf(OfflineMapFile(path, 0, TrailMap.FILE_ROLE_MAPSFORGE_MAP)),
                Instant.now(),
                CoordinateBounds(1.0, 1.0, -1.0, -1.0),
                null,
                parentId = groupId,
                visible = true
            )
        )
    }

    private fun canCreateMapFromCamera() {
        click(R.id.add_btn)
        click("Camera")
        click(R.id.capture_button)
        input(string(R.string.name), "Test Map", index = 1)
        clickOk()

        optional {
            click("Preview")
            click("Edit")
        }

        click("Next")
        clickOkIfShown()

        hasText("Test Map")
        hasText("Rotation: 0°")

        input("Location", "42, -72")
        click("Next")

        click(R.id.calibration_map, xPercent = 0.7f, yPercent = 0.3f)
        input("Location", "42.1, -72.1")
        click(string(R.string.preview))
        click("Finish calibration")

        hasText("Test Map")
        back()
    }

    private fun canCreateBlankMap(name: String) {
        click(R.id.add_btn)
        click(string(R.string.blank))

        clickOk()
        input(string(R.string.name), name, index = 1)
        clickOk()

        hasText(R.id.map_title, name)
        back()
    }

    private fun resolution(): Float {
        var resolution = 0f
        scenario.onActivity { resolution = it.findViewById<MapView>(R.id.map).resolutionPixels }
        return resolution
    }

    private fun measuredFeet(): Double {
        var text = ""
        scenario.onActivity {
            text = it.findViewById<Toolbar>(R.id.map_distance_title).subtitle.text.toString()
        }
        val (value, unit) = Regex("([\\d,.]+) (ft|mi)").find(text)!!.destructured
        val amount = value.replace(",", "").toDouble()
        return amount * if (unit == "mi") 5280.0 else 1.0
    }

    private fun textOf(id: Int): String {
        return waitFor {
            TestUtils.getMatchingChild(view(id).uiObject) { it.text != null }!!.text
        }
    }

    private fun parseCoordinate(text: String): Coordinate {
        val (latitude, longitude) = Regex("(-?\\d+\\.\\d+)°,\\s+(-?\\d+\\.\\d+)°")
            .find(text)!!.destructured
        return Coordinate(latitude.toDouble(), longitude.toDouble())
    }

    private fun distanceMeters(from: Coordinate, to: Coordinate): Double {
        val radius = 6371008.8
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * radius * atan2(sqrt(a), sqrt(1 - a))
    }

    companion object {
        private val location = Coordinate(42.03, -71.97)
    }
}
