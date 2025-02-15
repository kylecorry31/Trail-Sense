package com.kylecorry.trail_sense.tools.sensors

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolSensorsTest : ToolTestBase(Tools.SENSORS) {

    @Test
    fun verifyBasicFunctionality() {
        hasText(R.id.sensor_details_title, string(R.string.pref_sensor_details_title))

        // Verify it displays some sensors (Accelerometer and Battery are available on all test devices)
        hasText(string(R.string.accelerometer))
        hasText(string(R.string.tool_battery_title))

        // Click the sensor details button
        click(toolbarButton(R.id.sensor_details_title, Side.Right))

        hasText(string(R.string.sensors))
        hasText("android.sensor.accelerometer (1)")
        clickOk()

        isVisible(R.id.sensor_details_title)
    }
}