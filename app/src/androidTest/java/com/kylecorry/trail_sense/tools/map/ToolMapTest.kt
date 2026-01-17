package com.kylecorry.trail_sense.tools.map

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.longClick
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolMapTest : ToolTestBase(Tools.MAP) {

    @Test
    fun verifyBasicFunctionality() {
        // Disclaimer
        clickOk()

        canZoom()
        canLock()
        canLongPressMap()
        verifyMapMenuOptions()
    }

    @Test
    fun verifyMapLayers(){
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
        click("Slope")
        clickOk()

        // Close sheet
        click(toolbarButton(R.id.title, Side.Right))
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

}