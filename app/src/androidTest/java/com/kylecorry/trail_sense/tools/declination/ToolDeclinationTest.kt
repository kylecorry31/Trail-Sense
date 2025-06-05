package com.kylecorry.trail_sense.tools.declination

import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolDeclinationTest : ToolTestBase(Tools.DECLINATION) {

    @Test
    fun verifyBasicFunctionality() {
        // Wait for it to load the GPS location
        not(waitForTime = 15000) {
            hasText(string(R.string.loading), waitForTime = 0)
        }

        input(R.id.utm, "42, -72")

        // This varies with time, so look it up
        val declination1 = Geology.getGeomagneticDeclination(Coordinate(42.0, -72.0))
        val formatted1 = DecimalFormatter.format(declination1, 1)

        // Verify the declination is displayed
        hasText("$formatted1° (W)")

        input(R.id.utm, "-42, -72")

        // This varies with time, so look it up
        val declination2 = Geology.getGeomagneticDeclination(Coordinate(-42.0, -72.0))
        val formatted2 = DecimalFormatter.format(declination2, 1)

        // Verify the declination is displayed
        hasText("$formatted2° (E)")
    }
}