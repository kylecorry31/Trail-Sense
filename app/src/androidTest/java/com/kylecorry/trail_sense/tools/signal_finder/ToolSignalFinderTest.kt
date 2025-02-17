package com.kylecorry.trail_sense.tools.signal_finder

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.optional
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolSignalFinderTest : ToolTestBase(Tools.SIGNAL_FINDER, Coordinate(42.03, -71.97)) {

    @Test
    fun verifyBasicFunctionality() {
        hasText(string(R.string.cell_towers))

        // Detected signal
        optional {
            hasText(Regex("[0-9]G"))
            hasText(Regex("[0-9]+% • [0-9]+:[0-9]+:[0-9]+ (AM|PM) • (Full service | Emergency calls only)"))
        }

        // Tower
        hasText("4G")
        hasText("Cell tower • 0 ft • 0° N")
        hasText("Cell tower • 1.54 mi • 90° E")

        // Create a beacon at one of the cell towers
        TestUtils.clickListItemMenu(string(R.string.create_beacon))
        hasText(R.id.create_beacon_title, string(R.string.create_beacon))
        hasText(R.id.beacon_location, "42.030000°,  -71.970000°", contains = true)

        back()
        click("Leave")

        // Navigate to the tower
        TestUtils.clickListItemMenu(string(R.string.navigate))
        isVisible(R.id.navigation_sheet)
    }

}