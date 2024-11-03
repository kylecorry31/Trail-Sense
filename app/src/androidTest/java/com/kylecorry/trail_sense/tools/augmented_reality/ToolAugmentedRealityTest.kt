package com.kylecorry.trail_sense.tools.augmented_reality

import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isTrue
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.TestUtils.isCameraInUse
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@HiltAndroidTest
class ToolAugmentedRealityTest : ToolTestBase(Tools.AUGMENTED_REALITY) {

    @Test
    fun verifyBasicFunctionality() {
        if (!Tools.isToolAvailable(context, Tools.AUGMENTED_REALITY)) {
            return
        }

        // Click OK on the prompts
        clickOk()

        if (!Sensors.hasGyroscope(context)) {
            clickOk()
        }

        isTrue(10000) {
            isCameraInUse(isBackFacing = true)
        }

        canTurnOffCamera()
        canCalibrate()
        canToggleLayers()
    }

    private fun canTurnOffCamera() {
        click(R.id.camera_toggle)

        isTrue {
            !isCameraInUse(isBackFacing = true)
        }
    }

    private fun canCalibrate() {
        click(view(R.id.calibrate_btn))
        clickOk()
        click(R.id.confirm_calibration_button)
    }

    private fun canToggleLayers() {
        click(view(R.id.layers_btn))
        // Verify the layers panel is visible
        hasText(string(R.string.beacons))

        // Turn off the beacons layer
        click(string(R.string.visible), index = 0)

        // Close the layers panel
        click(toolbarButton(R.id.title, Side.Right))
        isNotVisible(R.id.title)
    }
}