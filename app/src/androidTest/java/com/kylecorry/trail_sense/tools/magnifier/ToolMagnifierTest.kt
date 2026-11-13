package com.kylecorry.trail_sense.tools.magnifier

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isTrue
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.TestUtils.isCameraInUse
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Assume.assumeTrue
import org.junit.Test

class ToolMagnifierTest : ToolTestBase(Tools.MAGNIFIER) {

    @Test
    fun startsThePreviewWithTheBackCamera() {
        assumeTrue(Tools.isToolAvailable(context, Tools.MAGNIFIER))

        isVisible(R.id.camera)
        isTrue(10000) {
            isCameraInUse(isBackFacing = true)
        }
    }

    @Test
    fun canFreezeAndResumeThePreviewAndToggleTheFocus() {
        assumeTrue(Tools.isToolAvailable(context, Tools.MAGNIFIER))

        // A frame can only be frozen once the preview is running
        isTrue(10000) {
            isCameraInUse(isBackFacing = true)
        }

        // Freeze button pauses the preview
        click(R.id.freeze_btn)
        isVisible(R.id.frozen_frame)
        isNotVisible(R.id.focus_toggle_btn)

        // Play button resumes the preview
        click(R.id.freeze_btn)
        not { isVisible(R.id.frozen_frame, waitForTime = 0) }
        isVisible(R.id.focus_toggle_btn)

        // Focus toggle switches between auto and close-up focus
        click(R.id.focus_toggle_btn)
        click(R.id.focus_toggle_btn)
    }
}
