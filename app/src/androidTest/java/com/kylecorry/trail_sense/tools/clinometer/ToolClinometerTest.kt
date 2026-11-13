package com.kylecorry.trail_sense.tools.clinometer

import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isTrue
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.optional
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.TestUtils.isCameraInUse
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Assume.assumeTrue
import org.junit.Test

class ToolClinometerTest : ToolTestBase(Tools.CLINOMETER) {

    @Test
    fun usesTheCameraUntilSwitchedToTheDialMode() {
        assumeTrue(Camera.hasBackCamera(context))

        // Starts with the camera enabled
        isTrue(10000) {
            isCameraInUse(isBackFacing = true)
        }

        click(toolbarButton(R.id.clinometer_title, Side.Left))

        isTrue {
            !isCameraInUse(isBackFacing = true)
        }

        optional {
            hasText("Hold your phone vertically", waitForTime = 1000)
            click(toolbarButton(R.id.clinometer_title, Side.Left))
        }
    }

    @Test
    fun showsTheInclinationAndCanBeLocked() {
        // The inclination comes from the emulator's sensors, so only the format is checked
        hasText(R.id.avalanche_risk, string(R.string.avalanche_risk))
        hasText(R.id.avalanche_risk, Regex("Low|Moderate|High"))

        hasText(R.id.clinometer_title, Regex("-?\\d+°"))
        hasText(R.id.clinometer_title, Regex("-?\\d+% slope"))

        hasText(R.id.estimated_height, string(R.string.distance_unset))
        hasText(R.id.estimated_height, string(R.string.height))

        // Lock
        click(R.id.incline_container)

        // TODO: Verify it is locked

        // Unlock
        click(R.id.incline_container)
    }

    @Test
    fun canEstimateTheDistanceToATarget() {
        click(toolbarButton(R.id.clinometer_title, Side.Right))

        click(string(R.string.distance))

        clickOk()

        input(R.id.amount, "5")
        input(R.id.secondary_amount, "5")

        clickOk()

        // Instructions dialog (5 ft 5 in is 5 + 5 / 12 = 5.42 ft)
        isNotVisible(R.id.amount)
        hasText("target that is 5.42 ft tall")
        clickOk()

        // The estimate depends on the emulator's inclination
        hasText(R.id.estimated_height, Regex("(-|\\d+(\\.\\d+)?) (mi|ft)"))
        hasText(R.id.estimated_height, "Distance")
    }

    @Test
    fun canEstimateTheHeightOfATarget() {
        click(toolbarButton(R.id.clinometer_title, Side.Right))

        click(string(R.string.height))

        clickOk()

        input(R.id.amount, "5")

        clickOk()

        // Instructions dialog
        isNotVisible(R.id.amount)
        hasText("target that is 5 ft away")
        clickOk()

        // The estimate depends on the emulator's inclination
        hasText(R.id.estimated_height, Regex("(-|\\d+(\\.\\d+)?) (mi|ft)"))
        hasText(R.id.estimated_height, "Height")
    }
}
