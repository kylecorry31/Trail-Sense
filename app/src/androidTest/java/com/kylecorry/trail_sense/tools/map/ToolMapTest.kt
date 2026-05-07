package com.kylecorry.trail_sense.tools.map

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.test_utils.AutomationLibrary
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.longClick
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.optional
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.clickListItemMenu
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.tools.map.domain.OfflineMapFile
import com.kylecorry.trail_sense.tools.map.domain.OfflineMapFileType
import com.kylecorry.trail_sense.tools.map.infrastructure.persistence.OfflineMapFileRepo
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.time.Instant

class ToolMapTest : ToolTestBase(Tools.MAP) {

    @Test
    fun verifyBasicFunctionality() {
        // Disclaimer
        clickOk()

        canZoom()
        canLock()
        canLongPressMap()
        verifyMapMenuOptions()
        verifySensorStatusBadges()
    }

    @Test
    fun verifyTimeSlider() {
        // Disclaimer
        clickOk()

        // Open the layers sheet
        click(R.id.menu_btn)
        click("Layers")

        // Enable Night layer (time dependent)
        scrollUntil { click("Additional layers") }
        click("Night")
        clickOk()

        // Close layers sheet
        click(toolbarButton(R.id.title, Side.Right))

        // Open time sheet
        click(R.id.time_btn)

        // Verify elements exist
        hasText(Regex("\\d{1,2}:\\d{2} [AP]M"))

        // Reset/Close
        click(R.id.time_btn)
    }

    @Test
    fun verifyMapLayers() {
        // Disclaimer
        clickOk()

        // Open the layers sheet
        click(R.id.menu_btn)
        click("Layers")

        // Verify the default layers are present
        scrollUntil { hasText("Location") }
        scrollUntil { hasText("Beacons") }
        scrollUntil { hasText("Paths") }
        scrollUntil { hasText("Tides") }
        scrollUntil { hasText("Navigation") }
        scrollUntil { hasText("Contours") }
        scrollUntil { hasText("Photo Maps") }
        scrollUntil { hasText("Hillshade") }
        scrollUntil { hasText("Elevation") }
        scrollUntil { hasText("Offline maps") }
        scrollUntil { hasText("Basemap") }

        // Add an additional layer (Slope layer)
        scrollUntil { hasText("Additional layers") }
        click("Additional layers")
        click("Slope")
        clickOk()

        // Verify the new layer appears
        hasText("Slope")

        // Expand the layer
        click("Slope")

        // Move the layer down
        click(R.id.layer_move_down)

        // Move the layer up
        click(R.id.layer_move_up)

        // Toggle visible (and then re-enable)
        click("Visible")
        click("Visible")
        scrollUntil { hasText("Opacity") }

        // Copy layer to other maps
        scrollUntil { hasText("Copy settings to other maps") }
        click("Copy settings to other maps")
        // Verify Navigation is checked, uncheck Photo Maps
        isChecked("Navigation")
        isChecked("Photo Maps")
        click("Photo Maps")
        isNotChecked("Photo Maps")
        clickOk()

        // Verify the "High resolution" toggle is disabled, then turn it on
        scrollUntil { hasText("High resolution") }
        click("High resolution")

        // Remove layer
        scrollUntil { hasText("Remove layer") }
        click("Remove layer")
        clickOk()

        // Re-add all layers
        scrollUntil { hasText("Additional layers") }
        click("Additional layers")
        click("Aspect")
        click("Cell towers")
        click("Lunar eclipse")
        click("Night")
        scrollUntil { click("Ruggedness") }
        scrollUntil { click("Sightings") }
        scrollUntil { hasText("Slope") }
        scrollUntil { click("Solar eclipse") }
        clickOk()

        // Close sheet
        click(toolbarButton(R.id.title, Side.Right))
    }

    @Test
    fun verifyOfflineMaps() {
        // TODO: To run on staging builds another solution to populating maps will need to be used
        if (AutomationLibrary.packageName != null) {
            return
        }

        // Disclaimer
        optional { clickOk() }

        openOfflineMaps()

        // Offline maps disclaimer
        optional { clickOk() }

        hasText(R.id.title, string(R.string.offline_maps))
        hasText(string(R.string.no_offline_maps))

        canOpenOfflineMapFilePicker()
        canCreateOfflineMapGroup()

        seedOfflineMap("Second Map")
        seedOfflineMap("Test Map")
        waitFor { hasText("Test Map") }
        hasText("Second Map")
        hasText("Mapsforge")

        canToggleOfflineMapVisibility()
        canViewOfflineMap()
        canRenameOfflineMapGroup()
        canSearchOfflineMaps()
        canRenameOfflineMap()
        canEditOfflineMapAttribution()
        canMoveOfflineMap()
        canDeleteOfflineMapGroup()
        canDeleteOfflineMap()
    }


    private fun verifySensorStatusBadges() {
        click(R.id.sensor_status_badges)
        hasText(string(R.string.accuracy_info_title))
        clickOk()
    }

    private fun canLock() {
        click(R.id.lock_btn)
        click(R.id.lock_btn)
        click(R.id.lock_btn)
    }

    private fun canZoom() {
        click(R.id.zoom_in_btn)
        click(R.id.zoom_out_btn)
    }

    private fun canLongPressMap() {
        longClick(R.id.map)
        hasText(Regex("-?\\d+\\.\\d+°,\\s+-?\\d+\\.\\d+°"))
        hasText(Regex("Elevation: -?\\d+(\\.\\d+)?\\s*(ft|m)"))
        hasText("Beacon")
        hasText("Navigate")
        hasText("Distance")

        click("Beacon")
        hasText("Create beacon")
        hasText(Regex(".*-?\\d+\\.\\d+°,\\s+-?\\d+\\.\\d+°.*"))
        back()
        click("Leave")
        longClick(R.id.map)

        click("Navigate")
        hasText(Regex(".*-?\\d+\\.\\d+°,\\s+-?\\d+\\.\\d+°.*"))
        click(toolbarButton(R.id.navigation_sheet_title, Side.Right))
        click("Yes")

        longClick(R.id.map)
        click("Distance")
        hasText("Distance")
        hasText(Regex("\\d+(\\.\\d+)? (mi|ft)"))
        hasText("Create path")
        click(toolbarButton(R.id.map_distance_title, Side.Right))
    }

    private fun verifyMapMenuOptions() {
        click(R.id.menu_btn)
        click("Measure")
        hasText("Distance")
        hasText(Regex("\\d+(\\.\\d+)? (mi|ft)"))
        hasText("Create path")
        click(toolbarButton(R.id.map_distance_title, Side.Right))

        click(R.id.menu_btn)
        click("Create path")
        hasText("Distance")
        hasText(Regex("\\d+(\\.\\d+)? (mi|ft)"))
        hasText("Create path")
        click(toolbarButton(R.id.map_distance_title, Side.Right))

        click(R.id.menu_btn)
        click("Layers")
        click(toolbarButton(R.id.title, Side.Right))
    }

    private fun openOfflineMaps() {
        click(R.id.menu_btn)
        click(string(R.string.layers))
        scrollUntil { hasText(string(R.string.offline_maps)) }
        click(string(R.string.offline_maps))
        scrollUntil { click(string(R.string.manage_maps)) }
        hasText(string(R.string.offline_maps))
    }

    private fun canOpenOfflineMapFilePicker() {
        click(R.id.add_btn)
        click(string(R.string.offline_map_file))

        // The file picker is opened
        isNotVisible(R.id.add_btn)

        waitFor {
            waitFor {
                back()
            }
            isVisible(R.id.title)
        }
    }

    private fun canCreateOfflineMapGroup() {
        click(R.id.add_btn)
        click(string(R.string.group))

        input(string(R.string.name), "Test Group", index = 1)
        clickOk()
        hasText("Test Group")
        hasText("0 maps")
    }

    private fun canToggleOfflineMapVisibility() {
        click(com.kylecorry.andromeda.views.R.id.trailing_icon_btn)
        click(com.kylecorry.andromeda.views.R.id.trailing_icon_btn)
    }

    private fun canViewOfflineMap() {
        click("Test Map")
        hasText("Test Map")
        click(R.id.zoom_in_btn)
        click(R.id.zoom_out_btn)
        back()
    }

    private fun canRenameOfflineMapGroup() {
        clickListItemMenu(string(R.string.rename), index = 0)
        input("Test Group", "Test Group 2")
        clickOk()
        hasText("Test Group 2")
        hasText("0 maps")
    }

    private fun canSearchOfflineMaps() {
        input(R.id.searchbox, "Second")
        hasText("Second Map")
        not { hasText("Test Map") }
        input(R.id.searchbox, "")
        hasText("Test Group 2")
        hasText("Test Map")
    }

    private fun canRenameOfflineMap() {
        clickListItemMenu(string(R.string.rename), index = 2)
        input("Test Map", "Test Map 2")
        clickOk()
        hasText("Test Map 2")
    }

    private fun canEditOfflineMapAttribution() {
        clickListItemMenu(string(R.string.attribution), index = 2)
        input(string(R.string.attribution), "Test Attribution")
        clickOk()
    }

    private fun canMoveOfflineMap() {
        clickListItemMenu(string(R.string.move_to), index = 2)
        click("Test Group 2")
        click(string(R.string.move))
        hasText("1 map")
        not { hasText("Test Map 2") }
    }

    private fun canDeleteOfflineMapGroup() {
        clickListItemMenu(string(R.string.delete), index = 0)
        clickOk()
        not { hasText("Test Group 2") }
        not { hasText("Test Map 2") }
    }

    private fun canDeleteOfflineMap() {
        clickListItemMenu(string(R.string.delete))
        clickOk()
        not { hasText("Second Map") }
        hasText(string(R.string.no_offline_maps))
    }

    private fun seedOfflineMap(name: String) = runBlocking {
        val files = getAppService<FileSubsystem>()
        files.getDirectory("offline_maps", true)
        val filename = name.lowercase().replace(" ", "-")
        val path = "offline_maps/$filename.map"
        files.get(path, true).writeBytes(ByteArray(0))

        getAppService<OfflineMapFileRepo>().add(
            OfflineMapFile(
                0,
                name,
                OfflineMapFileType.Mapsforge,
                path,
                0,
                Instant.now(),
                null,
                null,
                visible = true
            )
        )
    }

}
