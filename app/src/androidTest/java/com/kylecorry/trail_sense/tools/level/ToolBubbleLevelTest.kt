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
    fun showsTheAnglesAndTheLevel() {
        hasText(
            R.id.level_title,
            Regex(
                string(
                    R.string.bubble_level_angles,
                    "\\d+\\.\\d°",
                    "\\d+\\.\\d°",
                    "\\d+\\.\\d°"
                )
            )
        )

        isVisible(R.id.level)
    }
}
