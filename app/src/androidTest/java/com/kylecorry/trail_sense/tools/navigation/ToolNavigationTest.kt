package com.kylecorry.trail_sense.tools.navigation

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.any
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.longClick
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.OPTIONAL_WAIT_FOR_TIMEOUT
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.optional
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOkIfShown
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestData
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Assume.assumeFalse
import org.junit.Test

class ToolNavigationTest : ToolTestBase(Tools.NAVIGATION, Coordinate(42.03, -71.97)) {
    @Test
    fun verifyBasicFunctionality() {
        // Bearing
        hasText(Regex("\\s*\\d+°\\s+[NSEW]+"))
        // Location
        hasText(Regex("-?\\d+\\.\\d+°,\\s+-?\\d+\\.\\d+°"))
        // Elevation
        hasText(Regex("-?\\d+ ft"))
        // Speed
        hasText(Regex("\\d+\\.\\d+ mph"))

        canDisplaySensorStatus()

        // Compass
        canAdjustLayers()
        canSetDestinationBearing()
        canNavigate()
    }

    private fun canAdjustLayers() {
        optional(OPTIONAL_WAIT_FOR_TIMEOUT) {
            longClick(R.id.radar_compass)
            scrollUntil { hasText("Location") }
            scrollUntil { hasText("Beacons") }
            scrollUntil { hasText("Paths") }
            scrollUntil { hasText("Tides") }
            scrollUntil { hasText("Photo maps") }
            scrollUntil { hasText("Trail maps") }
            click(toolbarButton(R.id.title, Side.Right))
        }
    }

    private fun canSetDestinationBearing() {
        any(
            { click(R.id.radar_compass, waitForTime = 0) },
            { click(R.id.linear_compass, waitForTime = 0) }
        )

        hasText(R.id.navigation_sheet_title, Regex("\\d+° [NSEW]+"))
        isVisible(R.id.navigation_arrow)
        hasText(R.id.navigation_target_bearing, Regex("\\d+° [NSEW]+"))
        hasText(R.id.navigation_tertiary_label, string(R.string.navigation_facing))
        hasText(R.id.navigation_tertiary_value, Regex("\\d+° [NSEW]+"))

        cancelNavigation()
        not { isVisible(R.id.navigation_sheet) }
    }

    private fun canNavigate() {
        click(R.id.beaconBtn)

        // Create a beacon
        click(R.id.create_btn)
        click("Beacon", exact = true)
        input("Name", "Test Beacon")
        input("Location", "1.0, -1.0")
        input("Elevation", "100")
        scrollUntil { input(R.id.comment, "Test Comment") }
        click(toolbarButton(R.id.create_beacon_title, Side.Right))

        // Navigate to it
        isVisible(R.id.beacon_recycler)
        click("Test Beacon")
        hasText(R.id.beacon_title, "Test Beacon")
        click(string(R.string.navigate))
        clickOkIfShown()

        hasText("Test Beacon")
        hasText(Regex("1.000000°,\\s+-1.000000°"))
        hasText("100 ft")
        isVisible(R.id.navigation_arrow)
        hasText(R.id.navigation_primary_value, Regex("\\d+(\\.\\d+)? (ft|mi)"))
        hasText(R.id.navigation_secondary_value, Regex("(\\d+h)?\\s?(\\d+m)?\\s?(\\d+s)?"))
        hasText(R.id.navigation_secondary_label, Regex("Arrive \\d+:\\d+\\s(AM|PM)"))
        hasText(R.id.navigation_tertiary_value, Regex("\\d+ ft"))

        // The notes are shown on the beacon
        click(R.id.navigation_sheet_title)
        hasText(R.id.beacon_title, "Test Beacon")
        hasText("Test Comment")
        back()

        cancelNavigation()
        not { hasText("Test Beacon") }

        hasWorkingTrueNorthIndicator()

        hasWorkingQuickActions()

        canCreateBeacon()
    }

    private fun canDisplaySensorStatus() {
        click(Regex("(Poor|Moderate|Good|Stale|Unavailable)"))
        hasText(string(R.string.accuracy_info_title))
        clickOk()
    }

    private fun hasWorkingQuickActions() {
        click(toolbarButton(R.id.navigation_title, Side.Left))
        isVisible(R.id.paths_title)
        back()

        click(toolbarButton(R.id.navigation_title, Side.Right))
        clickOkIfShown()
        isVisible(R.id.map_list_title)
        back()
    }

    private fun hasWorkingTrueNorthIndicator() {
        click(R.id.north_reference_indicator)
        hasText(string(R.string.true_north))
        hasText(string(R.string.true_north_description))
        click(string(R.string.settings))
        hasText(string(R.string.pref_compass_sensor_title))
        back()
    }

    private fun canCreateBeacon() {
        longClick(R.id.beaconBtn)
        hasText(string(R.string.create_beacon))
        hasText(Regex("-?\\d+\\.\\d+°,\\s+-?\\d+\\.\\d+°"))
        back()
        click(string(R.string.dialog_leave))
        back()
    }

    // The current location is fixed at (42.03, -71.97) and the beacons are at known offsets from it
    // (verified with an independent calculation):
    // - Camp: 0.69 mi due north
    // - Summit: 40.40 mi at 36 degrees true (about 50 degrees magnetic, the declination is -14)

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
    }

    @Test
    fun showsTheCurrentLocation() {
        openWithSavedBeacons()

        hasText("42.030000°,  -71.970000°")
        hasText("0.0 mph")
        hasText(string(R.string.true_north))
    }

    @Test
    fun canNavigateToABeaconFromTheList() {
        openWithSavedBeacons()

        navigateToBeacon("Summit")

        hasText(R.id.navigation_sheet_title, "Summit")
        hasText("42.500000°,  -71.500000°")
        // Non-breaking spaces keep the elevation together if the subtitle wraps
        hasText("(Ele. 1000 ft)")
        hasText(R.id.navigation_primary_value, "40.41 mi")
        hasText(R.id.navigation_tertiary_value, "1000 ft")
        hasText(R.id.navigation_tertiary_label, string(R.string.navigation_remaining_gain))
        hasText(R.id.navigation_secondary_value, Regex("(\\d+h)?\\s?(\\d+m)?\\s?(\\d+s)?"))
    }

    @Test
    fun canSwitchTheDestination() {
        openWithSavedBeacons()

        navigateToBeacon("Summit")
        hasText(R.id.navigation_primary_value, "40.41 mi")

        cancelNavigation()
        not { hasText("Summit") }

        navigateToBeacon("Camp")
        hasText(R.id.navigation_sheet_title, "Camp")
        hasText(R.id.navigation_primary_value, "0.69 mi")
        not { hasText("Summit") }
    }

    private fun cancelNavigation() {
        click(R.id.navigation_cancel)
        // The undo snackbar covers the beacon button until it times out
        hasText(string(R.string.navigation_cancelled))
        not(waitForTime = 15000) { hasText(string(R.string.navigation_cancelled)) }
    }

    private fun navigateToBeacon(name: String) {
        click(R.id.beaconBtn)
        click(name)
        click(string(R.string.navigate))
        // Calibration reminder (only shown when the compass accuracy is poor)
        clickOkIfShown()
    }
}
