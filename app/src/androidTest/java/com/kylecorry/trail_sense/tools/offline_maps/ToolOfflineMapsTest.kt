package com.kylecorry.trail_sense.tools.offline_maps

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.test_utils.AutomationLibrary
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.backUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
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
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.OfflineMapFile
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.OfflineMapFileType
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.persistence.OfflineMapFileRepo
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.time.Instant

class ToolOfflineMapsTest : ToolTestBase(Tools.OFFLINE_MAPS) {

    @Test
    fun verifyMapsforgeBasicFunctionality() {
        // TODO: To run on staging builds another solution to populating maps will need to be used
        if (AutomationLibrary.packageName != null) {
            return
        }

        click("Mapsforge")

        // Offline maps disclaimer
        optional { clickOk() }

        hasText(R.id.title, string(R.string.mapsforge))
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

    @Test
    fun verifyPhotoMapsBasicFunctionality() {
        // Disclaimer
        clickOk()

        hasText(R.id.map_list_title, "Photo maps")

        // No maps by default
        hasText(string(R.string.no_maps))

        canCreateMapFromCamera()
        canToggleVisibility()
        canViewMap()
        canCreateBlankMap()
        canCreateMapFromFile()
        canCreateGroup()
        canRenameGroup()
        canSearch()
        canRenameMap()
        canMoveMap()
        canChangeMapResolution()
        canExportMap()
        canPrintMap()
        canDeleteGroup()
        canDeleteMap()
    }

    private fun canCreateMapFromCamera(goBack: Boolean = true) {
        click(R.id.add_btn)
        click("Camera")
        click(R.id.capture_button)
        input("Name", "Test Map", index = 1)
        clickOk()

        click("Preview")
        click("Edit")

        click("Next")
        optional {
            clickOk()
        }

        hasText("Test Map")
        hasText("Rotation: 0°")

        input("Location", "42, -72")
        click("Next")

        click(R.id.calibration_map, xPercent = 0.7f, yPercent = 0.3f)
        input("Location", "42.1, -72.1")
        click("Done")

        hasText("Test Map")
        if (goBack) {
            back()
        }
    }

    private fun canCreateMapFromFile() {
        click(R.id.add_btn)
        click(string(R.string.map_file))

        // The file picker is opened
        isNotVisible(R.id.add_btn)

        waitFor {
            waitFor {
                back()
            }
            isVisible(R.id.map_list_title)
        }
    }

    private fun canCreateBlankMap() {
        click(R.id.add_btn)
        click(string(R.string.blank))

        clickOk()
        input("Name", "Blank Map", index = 1)
        clickOk()

        hasText(R.id.map_title, "Blank Map")
        back()
    }

    private fun canCreateGroup() {
        click(R.id.add_btn)
        click(string(R.string.group))

        input("Name", "Test Group", index = 1)
        clickOk()
        hasText("Test Group")
        hasText("0 maps")
        click("Test Group")
        hasText(string(R.string.no_maps))

        // Create a map in the group
        canCreateBlankMap()

        hasText("Blank Map")
        back()
        hasText("Test Map")
        hasText("Test Group")
        hasText("1 map")
    }

    private fun canSearch() {
        input(R.id.searchbox, "Blank")
        hasText("Blank Map")
        hasText("Blank Map", index = 1)
        not { hasText("Test Map") }
        input(R.id.searchbox, "")
        hasText("Test Group 2")
    }

    private fun canDeleteGroup() {
        clickListItemMenu(string(R.string.delete), index = 0)
        clickOk()
        not { hasText("Test Group 2") }
    }

    private fun canRenameGroup() {
        clickListItemMenu(string(R.string.rename), index = 0)
        input("Test Group", "Test Group 2")
        clickOk()
        hasText("Test Group 2")
        hasText("1 map")
    }

    private fun canToggleVisibility() {
        click(com.kylecorry.andromeda.views.R.id.trailing_icon_btn)
        click(com.kylecorry.andromeda.views.R.id.trailing_icon_btn)
    }

    private fun canViewMap() {
        click("Test Map")
        hasText("Test Map")

        canZoom()
        canLock()
        canLongPressMap()
        verifyMapMenuOptions()
        back()
    }

    private fun canLock() {
        click(R.id.lock_btn)
        click(R.id.lock_btn)
        click(R.id.lock_btn)
        click(toolbarButton(R.id.map_title, Side.Left))
    }

    private fun canZoom() {
        click(R.id.zoom_in_btn)
        click(R.id.zoom_out_btn)
        click(toolbarButton(R.id.map_title, Side.Left))
    }

    private fun canLongPressMap() {
        longClick(R.id.map)
        hasText(Regex("-?\\d+\\.\\d+°,\\s+-?\\d+\\.\\d+°"))
        hasText(Regex("Elevation: -?\\d+(\\.\\d+)?\\s*(ft|m)"))
        hasText("Beacon")
        hasText("Navigate")
        hasText("Distance")

        click("Navigate")
        hasText("Test Map", index = 1)
        hasText(Regex("\\d+(\\.\\d+)? (mi|ft)"))
        click(toolbarButton(R.id.navigation_sheet_title, Side.Right))
        click("Yes")

        longClick(R.id.map)
        click("Distance")
        hasText("Distance")
        hasText(Regex("\\d+(\\.\\d+)? (mi|ft)"))
        hasText("Create path")
        click(toolbarButton(R.id.map_distance_title, Side.Right))

        longClick(R.id.map)
        click("Beacon")
        hasText("Create beacon")
        hasText(Regex(".*-?\\d+\\.\\d+°,\\s+-?\\d+\\.\\d+°.*"))
        back()
        click("Leave")
    }

    private fun verifyMapMenuOptions() {
        // Calibrate
        click(toolbarButton(R.id.map_title, Side.Right))
        click("Calibrate")
        hasText("Calibrate with known locations")
        click("Next")
        click("Done")
        isVisible(R.id.map)

        // User guide
        click(toolbarButton(R.id.map_title, Side.Right))
        click("User Guide")
        hasText("Photo maps")
        back()
        isVisible(R.id.map)

        // Rename
        click(toolbarButton(R.id.map_title, Side.Right))
        click("Rename")
        input("Test Map", "Test Map 2")
        clickOk()
        hasText("Test Map 2")

        // Change projection
        click(toolbarButton(R.id.map_title, Side.Right))
        click("Change projection")
        click("Equidistant")
        clickOk()

        // Measure
        click(toolbarButton(R.id.map_title, Side.Right))
        click("Measure")
        hasText("Distance")
        hasText(Regex("\\d+(\\.\\d+)? (mi|ft)"))
        hasText("Create path")
        click(toolbarButton(R.id.map_distance_title, Side.Right))

        // Create path
        click(toolbarButton(R.id.map_title, Side.Right))
        click("Create path")
        hasText("Distance")
        hasText(Regex("\\d+(\\.\\d+)? (mi|ft)"))
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

        // Export
        click(toolbarButton(R.id.map_title, Side.Right))
        click("Export")
        // Pressing back is needed sometimes to close the drive selector
        backUntil {
            hasText("test-map-2.pdf")
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

        // Delete
        click(toolbarButton(R.id.map_title, Side.Right))
        click(string(R.string.delete))
        clickOk()

        isVisible(R.id.map_list_title)
        not { hasText("Test Map 2") }

        // Recreate the map
        canCreateMapFromCamera(false)
    }

    private fun canRenameMap() {
        clickListItemMenu(string(R.string.rename), index = 2)
        input("Test Map", "Test Map 2")
        clickOk()
        hasText("Test Map 2")
    }

    private fun canMoveMap() {
        clickListItemMenu(string(R.string.move_to), index = 2)
        click("Test Group 2")
        click("Move")
        hasText("2 maps")
        not { hasText("Test Map 2") }
        click("Test Group 2")
        hasText("Test Map 2")
        back(false)
    }

    private fun canChangeMapResolution() {
        clickListItemMenu(string(R.string.change_resolution), index = 1)
        click("Moderate")
        clickOk()
        waitFor { hasText("Blank Map") }
    }

    private fun canExportMap() {
        clickListItemMenu(string(R.string.export), index = 1)
        // Pressing back is needed sometimes to close the drive selector
        backUntil {
            hasText("blank-map.pdf")
        }
        backUntil {
            isVisible(R.id.map_list_title)
        }
    }

    private fun canPrintMap() {
        clickListItemMenu(string(R.string.print), index = 1)
        hasText("Copies")
        backUntil {
            isVisible(R.id.map_list_title)
        }
    }

    private fun canDeleteMap() {
        clickListItemMenu(string(R.string.delete))
        clickOk()
        not { hasText("Blank Map") }
    }
}
