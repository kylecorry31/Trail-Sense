package com.kylecorry.trail_sense.tools.beacons

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollToEnd
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollToStart
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.TOOLBAR_RIGHT_BUTTON_ID
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@HiltAndroidTest
class ToolBeaconsTest : ToolTestBase(Tools.BEACONS) {

    @Test
    fun verifyBasicFunctionality() {
        hasText(R.id.beacon_title, string(R.string.beacons))

        // No beacons by default
        hasText(R.id.beacon_empty_text, string(R.string.no_beacons))

        createBeacon()
//        toggleVisibility()
//        openBeacon()
//        shareBeacon()
//        editBeacon()
//        navigate()
//        createGroup()
//        createBeaconInGroup()
//        search()
//        moveBeacon()
//        deleteBeacon()
//        renameGroup()
//        deleteGroup()
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


        // TODO: Figure out why this isn't working in the pipeline
//        click(TOOLBAR_RIGHT_BUTTON_ID)
//
//        hasText(R.id.beacon_title, string(R.string.beacons))
//        hasText("Test beacon")
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

    private fun shareBeacon() {}

    private fun editBeacon() {}

    private fun navigate() {}

    private fun deleteBeacon() {}

    private fun toggleVisibility() {}

    private fun createGroup() {}

    private fun createBeaconInGroup() {}

    private fun moveBeacon() {}

    private fun deleteGroup() {}

    private fun renameGroup() {}

    private fun search() {}
}