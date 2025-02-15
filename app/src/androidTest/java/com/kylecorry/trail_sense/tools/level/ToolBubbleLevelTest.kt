package com.kylecorry.trail_sense.tools.level

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolBubbleLevelTest : ToolTestBase(Tools.BUBBLE_LEVEL) {

    @Test
    fun verifyBasicFunctionality() {
        // Verify it shows the current angle
        hasText(
            R.id.level_title,
            Regex(
                string(
                    R.string.bubble_level_angles,
                    "\\d+\\.\\d°",
                    "\\d+\\.\\d°"
                )
            )
        )

        // Verify the level is shown (will fail if the level is not visible)
        isVisible(R.id.level)
    }
}