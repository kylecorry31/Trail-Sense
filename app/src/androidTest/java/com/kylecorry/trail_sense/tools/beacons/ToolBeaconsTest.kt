package com.kylecorry.trail_sense.tools.beacons

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasDataPoint
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasTextsInOrder
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.optional
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollToEnd
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestData
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.clickListItemMenu
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Assume.assumeFalse
import org.junit.Test

class ToolBeaconsTest : ToolTestBase(Tools.BEACONS, Coordinate(42.03, -71.97)) {

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
        click(string(R.string.beacon), exact = true)

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
        hasText(
            R.id.beacon_distance,
            Regex("\\d+\\.?\\d* (mi|ft)"),
            waitForTime = AutomationLibrary.GPS_WAIT_FOR_TIMEOUT
        )
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
        hasText(R.id.beacon_name, "Test beacon")
        hasText(R.id.beacon_location, "42.000000°,  -72.000000°")
        hasText(R.id.beacon_elevation, "1000")
        isNotChecked(R.id.create_at_distance)

        scrollToEnd(R.id.create_beacon_scroll)
        hasText(R.id.comment, "Test notes")
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
        not { hasText("Test beacon", exact = true) }
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
        not { hasText("Test beacon", exact = true) }

        click("Test group")
        hasText("Test beacon")
        hasText("Test beacon 2")
        back(false)
    }

    private fun deleteGroup() {
        clickListItemMenu("Delete")
        clickOk()
        hasText("No beacons")
        not { hasText("Test group", exact = true) }
    }

    private fun renameGroup() {
        clickListItemMenu("Rename")
        input("Test group", "Test group 2")
        clickOk()
        hasText("Test group 2")
        not { hasText("Test group", exact = true) }
    }

    private fun search() {
        input(R.id.searchbox, "2")
        hasText("Test beacon 2")
        input(R.id.searchbox, "")
        hasText("Test group")
    }

    // Beacons are placed at known offsets from the current location (42.03, -71.97) so the distances
    // and directions can be checked. They were verified by an independent calculation:
    // - Camp: 0.69 mi due north
    // - Parking: 4.15 mi at 120 degrees
    // - Lake: 14.84 mi at 233 degrees
    // - Summit: 40.40 mi at 36 degrees

    private fun openWithSavedBeacons() {
        assumeFalse(isStagingBuild)
        relaunchTool { seedBeacons() }
    }

    private fun seedBeacons() {
        TestData.addBeacon(
            "Summit",
            Coordinate(42.5, -71.5),
            elevation = 304.8f,
            comment = "Great view"
        )
        TestData.addBeacon("Camp", Coordinate(42.04, -71.97), elevation = 100f)
        TestData.addBeacon("Lake", Coordinate(41.9, -72.2))
        val group = TestData.addBeaconGroup("Trailheads")
        TestData.addBeacon("Parking", Coordinate(42.0, -71.9), groupId = group)
    }

    @Test
    fun listIsSortedByDistanceFromTheCurrentLocation() {
        openWithSavedBeacons()

        hasTextsInOrder(
            com.kylecorry.andromeda.views.R.id.title,
            listOf("Camp", "Trailheads", "Lake", "Summit")
        )
        hasTextsInOrder(
            com.kylecorry.andromeda.views.R.id.description,
            listOf("0.69 mi", "1 beacon", "14.86 mi", "40.41 mi")
        )
    }

    @Test
    fun groupsListTheirBeacons() {
        openWithSavedBeacons()

        click("Trailheads")

        hasText(R.id.beacon_title, "Trailheads")
        hasText("Parking")
        // 4.15 mi on a spherical earth
        hasText(Regex("4\\.1[56] mi"))
        not { hasText("Camp") }
    }

    @Test
    fun canSearchByName() {
        openWithSavedBeacons()

        input(R.id.searchbox, "lak")
        hasText("Lake")
        not { hasText("Summit") }
        not { hasText("Camp") }

        // Beacons inside of groups are found too
        input(R.id.searchbox, "park")
        hasText("Parking")
        not { hasText("Lake") }
    }

    @Test
    fun detailsShowTheSavedBeacon() {
        openWithSavedBeacons()

        click("Summit")

        hasText(R.id.beacon_title, "Summit")
        hasText(R.id.beacon_title, "42.500000°,  -71.500000°")
        // Stored in meters, displayed in feet
        hasDataPoint("1000 ft", string(R.string.elevation))
        hasDataPoint("40.41 mi", string(R.string.distance))
        hasText(R.id.comment_text, "Great view")
    }

    @Test
    fun canNavigateToABeacon() {
        openWithSavedBeacons()

        click("Summit")
        click(string(R.string.navigate))
        clickOk()

        hasText(R.id.navigation_sheet_title, "Summit")
        hasText(R.id.navigation_distance, "40.41 mi")
        hasText(R.id.navigation_distance, "36° NE")
    }

    @Test
    fun canCreateABeaconAtADistanceAndBearing() {
        openWithSavedBeacons()

        click(R.id.create_btn)
        click(string(R.string.beacon), exact = true)
        input(R.id.beacon_name, "Mile north")
        input(R.id.beacon_location, "42, -72")
        click(R.id.create_at_distance)
        isChecked(R.id.create_at_distance)

        input(string(R.string.distance), "5280")
        click(string(R.string.enter_manually))
        input(R.id.bearing, "0")
        click(R.id.true_north)
        clickOk()

        scrollToEnd(R.id.create_beacon_scroll)
        click(toolbarButton(R.id.create_beacon_title, Side.Right))

        // One mile north of 42, -72
        click("Mile north")
        hasText(R.id.beacon_title, Regex("42\\.0144\\d\\d°,\\s+-72\\.0000\\d\\d°"))
        back()
    }

}
