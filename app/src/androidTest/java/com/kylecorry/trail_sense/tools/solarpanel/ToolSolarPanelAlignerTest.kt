package com.kylecorry.trail_sense.tools.solarpanel

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolSolarPanelAlignerTest : ToolTestBase(Tools.SOLAR_PANEL_ALIGNER) {

    @Test
    fun verifyBasicFunctionality() {
        if (!Tools.isToolAvailable(context, Tools.SOLAR_PANEL_ALIGNER)) {
            return
        }

        clickOk()

        // It should show the solar panel details for today
        hasText(R.id.tilt_label, string(R.string.tilt))
        hasText(R.id.current_altitude, Regex("-?\\d+°"))
        hasText(R.id.desired_altitude, Regex("-?\\d+°"))

        hasText(R.id.azimuth_label, string(R.string.compass_azimuth))
        hasText(R.id.current_azimuth, Regex("\\d+°"))
        hasText(R.id.desired_azimuth, Regex("\\d+°"))

        hasText(R.id.energy, Regex("Up to \\d+(\\.\\d+)? kWh / m²"))

        // TODO: Verify that today is selected

        // It should show the solar panel details for a custom time
        click(R.id.solar_now_btn)

        clickOk()

        hasText(R.id.tilt_label, string(R.string.tilt))
        hasText(R.id.current_altitude, Regex("-?\\d+°"))
        hasText(R.id.desired_altitude, Regex("-?\\d+°"))

        hasText(R.id.azimuth_label, string(R.string.compass_azimuth))
        hasText(R.id.current_azimuth, Regex("\\d+°"))
        hasText(R.id.desired_azimuth, Regex("\\d+°"))

        hasText(R.id.energy, Regex("Up to \\d+(\\.\\d+)? kWh / m²"))

        // TODO: Verify that the now button is selected
        hasText(R.id.solar_now_btn, "2h")
    }
}