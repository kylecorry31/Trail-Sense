package com.kylecorry.trail_sense.tools.declination

import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.GPS_WAIT_FOR_TIMEOUT
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test
import kotlin.math.abs

class ToolDeclinationTest : ToolTestBase(Tools.DECLINATION) {

    @Test
    fun showsTheDeclinationOfAnEnteredLocation() {
        // Wait for it to load the GPS location
        not(waitForTime = GPS_WAIT_FOR_TIMEOUT) {
            hasText(string(R.string.loading))
        }

        // NOAA WMM declination on 2026-11-13: -13.51 degrees at 42, -72 and 6.65 degrees at
        // -42, -72. It changes by about 0.1 degrees a year, so allow 0.3 degrees of drift
        input(R.id.utm, "42, -72")
        hasDeclination(-13.51, "W")

        input(R.id.utm, "-42, -72")
        hasDeclination(6.65, "E")
    }

    private fun hasDeclination(expected: Double, direction: String) {
        val allowed = (-3..3).joinToString("|") {
            Regex.escape(DecimalFormatter.format(abs(expected) + it / 10.0, 1))
        }
        hasText(Regex("($allowed)° \\($direction\\)"))
    }
}
