package com.kylecorry.trail_sense.tools.beacons

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.optional
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollToEnd
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.clickListItemMenu
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolBeaconsTest : ToolTestBase(Tools.BEACONS) {

    @Test
    fun verifyBasicFunctionality() {
        hasText(R.id.beacon_title, string(R.string.beacons))

        // No beacons by default
        hasText(R.id.beacon_empty_text, string(R.string.no_beacons))

        createBeacon()
        openBeacon()
        shareBeacon()
        editBeacon()
        navigate()
        createGroup()
        moveBeacon()
        search()
        deleteBeacon()
        toggleVisibility()
        renameGroup()
        deleteGroup()
    }

    private fun createBeacon() {
        click(R.id.create_btn)
        click(string(R.string.beacon))

        hasText(R.id.create_beacon_title, string(R.string.create_beacon))

        input(R.id.beacon_name, "Test beacon")
        input(R.id.beacon_location, "42, -72")
        input(R.id.beacon_elevation, "1000")

        isNotChecked(R.id.create_at_distance)

        scrollToEnd(R.id.create_beacon_scroll)

        hasText(R.id.beacon_group_picker, string(R.string.no_group))
        hasText(R.id.beacon_color_picker, string(R.string.color))
        hasText(R.id.beacon_icon_picker, string(R.string.icon))

        input(R.id.comment, "Test notes")

        click(toolbarButton(R.id.create_beacon_title, Side.Right))

        hasText(R.id.beacon_title, string(R.string.beacons))
        hasText("Test beacon")
    }

    private fun openBeacon() {
        click("Test beacon")
        hasText(R.id.beacon_title, "Test beacon")
        hasText(R.id.beacon_title, "42.000000°,  -72.000000°")

        hasText(R.id.beacon_altitude, "1000 ft")
        hasText(R.id.beacon_altitude, string(R.string.elevation))
        hasText(R.id.beacon_distance, Regex("\\d+\\.?\\d* (mi|ft)"))
        hasText(R.id.beacon_distance, string(R.string.distance))
        hasText(R.id.beacon_temperature, Regex("\\d+ °F / \\d+ °F"))
        hasText(R.id.beacon_temperature, string(R.string.temperature_high_low))
        hasText(R.id.beacon_sunrise, Regex("\\d+:\\d+ (AM|PM)"))
        hasText(R.id.beacon_sunrise, string(R.string.sunrise))
        hasText(R.id.beacon_sunset, Regex("\\d+:\\d+ (AM|PM)"))
        hasText(R.id.beacon_sunset, string(R.string.sunset))
        hasText(R.id.beacon_tide, Regex("(High|Low|Half)"))
        hasText(R.id.beacon_tide, string(R.string.tide))
        hasText(R.id.comment_text, "Test notes")
    }

    private fun shareBeacon() {
        click(toolbarButton(R.id.beacon_title, Side.Right))
        click(string(R.string.share_ellipsis))

        hasText(string(android.R.string.copy))
        hasText(string(R.string.qr_code))
        hasText(string(R.string.maps))
        hasText(string(R.string.share_action_send))

        back(false)
    }

    private fun editBeacon() {
        click(R.id.edit_btn)
        hasText(R.id.create_beacon_title, string(R.string.create_beacon))
        hasText(R.id.beacon_name, "Test beacon", contains = true)
        hasText(R.id.beacon_location, "42.000000°,  -72.000000°", contains = true)
        hasText(R.id.beacon_elevation, "1000", contains = true)
        isNotChecked(R.id.create_at_distance)

        scrollToEnd(R.id.create_beacon_scroll)
        hasText(R.id.comment, "Test notes", contains = true)
        hasText(R.id.beacon_group_picker, string(R.string.no_group))
        hasText(R.id.beacon_color_picker, string(R.string.color))
        hasText(R.id.beacon_icon_picker, string(R.string.icon))

        input(R.id.comment, "Test notes 2")

        click(toolbarButton(R.id.create_beacon_title, Side.Right))

        hasText(R.id.beacon_title, "Test beacon")
        hasText(R.id.comment_text, "Test notes 2")
    }

    private fun navigate() {
        click(string(R.string.navigate))

        optional {
            clickOk()
        }

        isVisible(R.id.navigation_title)
        hasText("Test beacon")
        back()
        back()
    }

    private fun deleteBeacon() {
        click("Test group")
        clickListItemMenu("Delete")
        clickOk()
        hasText("Test beacon 2")
        not { hasText("Test beacon", waitForTime = 0) }
        back(false)
    }

    private fun toggleVisibility() {
        click("Test group")
        click(com.kylecorry.andromeda.views.R.id.trailing_icon_btn)
        click(com.kylecorry.andromeda.views.R.id.trailing_icon_btn)
        back(false)
    }

    private fun createGroup() {
        click(R.id.create_btn)
        click(string(R.string.group))

        input(string(R.string.name), "Test group")
        clickOk()

        hasText("Test group")
        hasText("0 beacons")
        click("Test group")
        hasText("No beacons")

        createBeaconInGroup()
    }

    private fun createBeaconInGroup() {
        click(R.id.create_btn)
        click(string(R.string.beacon))

        input(R.id.beacon_name, "Test beacon 2")
        input(R.id.beacon_location, "42, -72")

        scrollToEnd(R.id.create_beacon_scroll)
        hasText("Test group")

        click(toolbarButton(R.id.create_beacon_title, Side.Right))

        hasText(R.id.beacon_title, "Test group")
        hasText("Test beacon 2")
        back(false)
        hasText("Test group")
        hasText("1 beacon")
        hasText("Test beacon")
    }

    private fun moveBeacon() {
        clickListItemMenu("Move to")
        click("Test group")
        click("Move")
        hasText("2 beacons")
        not { hasText("Test beacon", waitForTime = 0) }

        click("Test group")
        hasText("Test beacon")
        hasText("Test beacon 2")
        back(false)
    }

    private fun deleteGroup() {
        clickListItemMenu("Delete")
        clickOk()
        hasText("No beacons")
        not { hasText("Test group", waitForTime = 0) }
    }

    private fun renameGroup() {
        clickListItemMenu("Rename")
        input("Test group", "Test group 2")
        clickOk()
        hasText("Test group 2")
        not { hasText("Test group", waitForTime = 0) }
    }

    private fun search() {
        input(R.id.searchbox, "2")
        hasText("Test beacon 2")
        input(R.id.searchbox, "")
        hasText("Test group")
    }
}