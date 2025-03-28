package com.kylecorry.trail_sense.tools.metaldetector

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.TestUtils.mute
import com.kylecorry.trail_sense.test_utils.TestUtils.unmute
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolMetalDetectorTest : ToolTestBase(Tools.METAL_DETECTOR) {

    @Test
    fun verifyBasicFunctionality() {
        if (!Tools.isToolAvailable(context, Tools.METAL_DETECTOR)) {
            return
        }

        // Verify the title
        hasText(R.id.metal_detector_title, Regex("\\d+ uT"))

        // Make sure the chart is visible
        isVisible(R.id.metal_chart)

        // Can turn on high sensitivity
        isNotChecked(R.id.high_sensitivity_toggle)
        click(R.id.high_sensitivity_toggle)
        isChecked(R.id.high_sensitivity_toggle)

        // Can adjust threshold (just verify it is set)
        isVisible(R.id.threshold)
        hasText(R.id.threshold_amount, "5.0 uT")

        // Can play sound
        val originalVolume = mute()
        click(toolbarButton(R.id.metal_detector_title, Side.Right))
        click(toolbarButton(R.id.metal_detector_title, Side.Right))
        unmute(originalVolume)

        // Can disable vibration
        var buttonVibrationToggle = toolbarButton(R.id.metal_detector_title, Side.Left)
        click(buttonVibrationToggle)
        // TODO: assert button state and vibration subsystem state

    }
}