package com.kylecorry.trail_sense.tools.photo_maps

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.longClick
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils.back
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
        canViewMap()
        canCreateBlankMap()
        canCreateMapFromFile()
        canGroup()
        canSearch()
        verifyMapListItemOptions()
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
        // TODO
    }

    private fun canCreateBlankMap() {
        // TODO
    }

    private fun canGroup() {
        // TODO
    }

    private fun canSearch() {
        // TODO
    }

    private fun verifyMapListItemOptions() {
        // TODO
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
    }


}