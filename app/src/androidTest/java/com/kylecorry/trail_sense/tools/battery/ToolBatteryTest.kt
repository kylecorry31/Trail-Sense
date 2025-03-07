package com.kylecorry.trail_sense.tools.battery

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolBatteryTest : ToolTestBase(Tools.BATTERY) {

    @Test
    fun verifyBasicFunctionality() {
        hasText(Regex("\\d+%"))
        hasText(Regex("(Charging|Discharging).*"))
        hasText("Battery log")
        hasText("Updates every 1h - Low battery usage")

        isNotChecked(R.id.low_power_mode_switch)
        click(R.id.low_power_mode_switch)
        isChecked(R.id.low_power_mode_switch)

        // Stop the battery log
        click(R.id.disable_btn)
        not { hasText("Battery log", waitForTime = 0) }

        // Open battery settings
        click(toolbarButton(R.id.battery_title, Side.Right))
        hasText("Battery", contains = true)
    }
}