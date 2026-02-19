package com.kylecorry.trail_sense.tools.cliffheight

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolCliffHeightTest : ToolTestBase(Tools.CLIFF_HEIGHT) {
    @Test
    fun verifyBasicFunctionality() {
        // TODO: This would need to be enabled to run on staging builds
        if (AutomationLibrary.packageName != null) {
            return
        }

        // Accept the disclaimer
        clickOk()
        click(R.id.start_btn)
        hasText(R.id.cliff_height_title, Regex("[\\d.]+ ft"))
        click(R.id.start_btn)
        hasText(R.id.cliff_height_title, Regex("[\\d.]+ ft"))
    }
}