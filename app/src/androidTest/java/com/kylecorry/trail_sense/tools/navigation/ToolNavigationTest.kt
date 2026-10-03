package com.kylecorry.trail_sense.tools.navigation

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.any
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.longClick
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.optional
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestData
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Assert.assertEquals
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
        optional {
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

        hasText("Bearing")
        isVisible(R.id.navigation_bearing)

        click(toolbarButton(R.id.navigation_sheet_title, Side.Right))
        click("Yes")
        not { hasText("Bearing") }
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

        hasText(string(R.string.calibrate_compass_dialog_title))
        clickOk()

        hasText("Test Beacon")
        hasText(Regex("1.000000°,\\s+-1.000000°"))
        hasText("100 ft")
        hasText(R.id.navigation_distance, Regex("\\d+(\\.\\d+)? (ft|mi)"))
        hasText(R.id.navigation_distance, Regex("\\d+° [NSEW]+"))
        hasText(R.id.navigation_eta, Regex("(\\d+h)?\\s?(\\d+m)?\\s?(\\d+s)?"))
        hasText(R.id.navigation_eta, Regex("\\d+:\\d+?\\s(AM|PM)"))
        hasText(R.id.navigation_elevation, Regex("[-+]?\\d+ ft"))
        click(toolbarButton(R.id.navigation_sheet_title, Side.Left))
        hasText("Test Comment")
        clickOk()

        click("Test Beacon")
        hasText(R.id.beacon_title, "Test Beacon")
        back()

        click(toolbarButton(R.id.navigation_sheet_title, Side.Right))
        click("Yes")
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
        optional { clickOk() }
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
        hasText("Elevation: 1000 ft")
        hasText(R.id.navigation_distance, "40.41 mi")
        hasText(R.id.navigation_distance, "36° NE")
        hasText(R.id.navigation_elevation, "+1000 ft")
        hasText(R.id.navigation_eta, Regex("(\\d+h)?\\s?(\\d+m)?\\s?(\\d+s)?"))
    }

    @Test
    fun canSwitchTheDestination() {
        openWithSavedBeacons()

        navigateToBeacon("Summit")
        hasText(R.id.navigation_distance, "40.41 mi")

        click(toolbarButton(R.id.navigation_sheet_title, Side.Right))
        click(string(R.string.yes))
        not { hasText("Summit") }

        navigateToBeacon("Camp")
        hasText(R.id.navigation_sheet_title, "Camp")
        hasText(R.id.navigation_distance, "0.69 mi")
        hasText(R.id.navigation_distance, "0° N")
        not { hasText("Summit") }
    }

    @Test
    fun directionIsRelativeToMagneticNorthWhenSelected() {
        assumeFalse(isStagingBuild)
        relaunchTool {
            seedBeacons()
            PreferencesSubsystem.getInstance(TestUtils.context).preferences.putBoolean(
                string(R.string.pref_use_true_north),
                false
            )
        }
        click(R.id.beaconBtn)
        click("Summit")
        click(string(R.string.navigate))
        clickOk()

        // True bearing of 36 degrees minus the declination of about -14 degrees
        waitFor {
            val text = textOf(R.id.navigation_distance)
            val direction = Regex("(\\d+)° ([NSEW]+)").find(text)!!.groupValues[1].toInt()
            assertEquals(50.0, direction.toDouble(), 3.0)
        }
        hasText(string(R.string.magnetic_north))
    }

    @Test
    fun beaconCommentCanBeViewedWhileNavigating() {
        openWithSavedBeacons()

        navigateToBeacon("Summit")

        click(toolbarButton(R.id.navigation_sheet_title, Side.Left))
        hasText("Great view")
        clickOk()
        isVisible(R.id.navigation_sheet_title)
    }

    private fun navigateToBeacon(name: String) {
        click(R.id.beaconBtn)
        click(name)
        click(string(R.string.navigate))
        // Calibration reminder
        clickOk()
    }

    private fun textOf(id: Int): String {
        return waitFor {
            val texts = mutableListOf<String>()
            val root = view(id).uiObject
            TestUtils.matchesSelfOrChild(root) {
                it.text?.let { text -> texts.add(text) }
                false
            }
            texts.joinToString(" ")
        }
    }
}
