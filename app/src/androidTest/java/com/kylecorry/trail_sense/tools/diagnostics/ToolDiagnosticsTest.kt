package com.kylecorry.trail_sense.tools.diagnostics

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolDiagnosticsTest : ToolTestBase(Tools.DIAGNOSTICS) {

    @Test
    fun verifyBasicFunctionality() {
        hasText(R.id.diagnostics_title, string(R.string.diagnostics))

        // Battery usage always starts restricted, so just check for that
        hasText(string(R.string.battery_usage_restricted), waitForTime = 15000)
        click(string(R.string.battery_usage_restricted))
        click(string(android.R.string.cancel))

        click(toolbarButton(R.id.diagnostics_title, Side.Right))
        hasText(R.id.sensor_details_title, string(R.string.pref_sensor_details_title))
    }

}