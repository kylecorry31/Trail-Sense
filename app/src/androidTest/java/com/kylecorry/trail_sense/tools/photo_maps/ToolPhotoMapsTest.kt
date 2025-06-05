package com.kylecorry.trail_sense.tools.photo_maps

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.longClick
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.clickListItemMenu
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolPhotoMapsTest : ToolTestBase(Tools.PHOTO_MAPS) {

    @Test
    fun verifyBasicFunctionality() {
        // Disclaimer
        clickOk()

        hasText(R.id.map_list_title, "Photo Maps")

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

    private fun canCreateMapFromCamera() {
        click(R.id.add_btn)
        click("Camera")
        click(R.id.capture_button)
        input("Name", "Test Map", index = 1, contains = true)
        clickOk()

        click("Preview")
        click("Edit")

        click("Next")
        clickOk()

        hasText("Test Map")
        hasText("Rotation: 0°")

        input("Location", "42, -72")
        click("Next")

        click(R.id.calibration_map, xPercent = 0.7f, yPercent = 0.3f)
        input("Location", "42.1, -72.1")
        click("Done")

        hasText("Test Map")
        back()
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
        input("Name", "Blank Map", index = 1, contains = true)
        clickOk()

        hasText(R.id.map_title, "Blank Map")
        back()
    }

    private fun canCreateGroup() {
        click(R.id.add_btn)
        click(string(R.string.group))

        input("Name", "Test Group", index = 1, contains = true)
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
        not { hasText("Test Map", waitForTime = 0) }
        input(R.id.searchbox, "")
        hasText("Test Group 2")
    }

    private fun canDeleteGroup() {
        clickListItemMenu(string(R.string.delete), index = 0)
        clickOk()
        not { hasText("Test Group 2", waitForTime = 0) }
    }

    private fun canRenameGroup() {
        clickListItemMenu(string(R.string.rename), index = 0)
        input("Test Group", "Test Group 2", contains = true)
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
        hasText("Test Map", index = 1)
        hasText(Regex("\\d+(\\.\\d+)? mi|ft"))
        click(R.id.cancel_navigation_btn)

        longClick(R.id.map)
        click("Distance")
        hasText("Distance")
        hasText(Regex("\\d+(\\.\\d+)? mi|ft"))
        hasText("Create path")
        click(toolbarButton(R.id.map_distance_title, Side.Right))
    }

    private fun verifyMapMenuOptions() {
        // TODO
        // Calibrate
        // User Guide
        // Rename
        // Change projection
        // Measure
        // Create path
        // Export
        // Print
        // Delete
    }

    private fun canRenameMap() {
        clickListItemMenu(string(R.string.rename), index = 2)
        input("Test Map", "Test Map 2", contains = true)
        clickOk()
        hasText("Test Map 2")
    }

    private fun canMoveMap() {
        clickListItemMenu(string(R.string.move_to), index = 2)
        click("Test Group 2")
        click("Move")
        hasText("2 maps")
        not { hasText("Test Map 2", waitForTime = 0) }
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
        hasText("blank-map.pdf", contains = true)
        waitFor {
            waitFor {
                back()
            }
            isVisible(R.id.map_list_title)
        }
    }

    private fun canPrintMap() {
        clickListItemMenu(string(R.string.print), index = 1)
        hasText("Copies", contains = true)
        waitFor {
            waitFor {
                back()
            }
            isVisible(R.id.map_list_title)
        }
    }

    private fun canDeleteMap() {
        clickListItemMenu(string(R.string.delete))
        clickOk()
        not { hasText("Blank Map", waitForTime = 0) }
    }

}