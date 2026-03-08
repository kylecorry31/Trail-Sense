package com.kylecorry.trail_sense.tools.magnifier

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isTrue
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.TestUtils.isCameraInUse
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolMagnifierTest : ToolTestBase(Tools.MAGNIFIER) {

    @Test
    fun verifyBasicFunctionality() {
        if (!Tools.isToolAvailable(context, Tools.MAGNIFIER)) {
            return
        }

        // Camera view is visible and the back camera starts
        isVisible(R.id.camera)
        isTrue(10000) {
            isCameraInUse(isBackFacing = true)
        }

        // Freeze button pauses the preview
        click(R.id.freeze_btn)
        isVisible(R.id.frozen_frame)

        // Play button resumes the preview
        click(R.id.freeze_btn)
        not { isVisible(R.id.frozen_frame, waitForTime = 0) }

        // Focus toggle switches between auto and close-up focus
        click(R.id.focus_toggle_btn)
        click(R.id.focus_toggle_btn)
    }
}
