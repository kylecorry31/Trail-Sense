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
    fun showsTheDetectedSignalAndNearbyTowers() {
        hasText(string(R.string.cell_towers))

        optional {
            hasText(Regex("[0-9]G"))
            hasText(Regex("[0-9]+% •(\\d+\\.\\d+ (ft|mi) - \\d+\\.\\d+ (ft|mi) •)? [0-9]+:[0-9]+:[0-9]+ (AM|PM) • (Full service | Emergency calls only)"))
        }

        // Tower (the dataset is bundled with the app, so the exact tower can't be seeded)
        hasText("Cell tower")
        hasText(Regex("\\d+(\\.\\d+)? (ft|mi) • \\d+° [NESW]+"))
    }

    @Test
    fun canNavigateToATower() {
        hasText("Cell tower")

        TestUtils.clickListItemMenu(string(R.string.navigate))
        isVisible(R.id.navigation_sheet)
    }

}
