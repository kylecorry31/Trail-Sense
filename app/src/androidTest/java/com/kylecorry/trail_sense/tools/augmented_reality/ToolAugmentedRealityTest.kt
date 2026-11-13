package com.kylecorry.trail_sense.tools.augmented_reality

import com.kylecorry.andromeda.camera.Camera
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
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Assume.assumeTrue
import org.junit.Test

class ToolAugmentedRealityTest : ToolTestBase(Tools.AUGMENTED_REALITY) {

    private fun acceptThePrompts() {
        assumeTrue(Tools.isToolAvailable(context, Tools.AUGMENTED_REALITY))

        // Click OK on the prompts
        clickOk()

        if (!Sensors.hasGyroscope(context)) {
            clickOk()
        }
    }

    @Test
    fun usesTheBackCameraUntilItIsTurnedOff() {
        assumeTrue(Camera.hasBackCamera(context))
        acceptThePrompts()

        isTrue(10000) {
            isCameraInUse(isBackFacing = true)
        }

        click(R.id.camera_toggle)

        isTrue {
            !isCameraInUse(isBackFacing = true)
        }
    }

    @Test
    fun canCalibrate() {
        acceptThePrompts()

        click(R.id.calibrate_btn)
        clickOk()
        click(R.id.confirm_calibration_button)
    }

    @Test
    fun canSearchForAnAstronomyObject() {
        acceptThePrompts()

        click(R.id.ar_search_btn)
        click(string(R.string.astronomy))
        clickOk()
        hasText(string(R.string.locate))
        click(string(android.R.string.cancel))
    }

    @Test
    fun canToggleTheLayers() {
        acceptThePrompts()

        click(R.id.layers_btn)
        // Verify the layers panel is visible
        hasText(string(R.string.beacons))
        hasText(string(R.string.ar_path_appearance))

        // Turn off the beacons layer
        click(string(R.string.visible), index = 0)

        // Close the layers panel
        click(toolbarButton(R.id.title, Side.Right))
        isNotVisible(R.id.title)
    }
}
