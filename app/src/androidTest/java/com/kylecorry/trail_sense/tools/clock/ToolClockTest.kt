package com.kylecorry.trail_sense.tools.clock

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ActivityScenario
import androidx.test.uiautomator.By
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.handleExactAlarmsDialog
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolClockTest: ToolTestBase(Tools.CLOCK) {

    @Test
    fun verifyBasicFunctionality() {
        // Verify the title
        hasText(R.id.clock_title, Regex("\\d{1,2}:\\d{2}:\\d{2} [AP]M"))
        hasText(R.id.clock_title, Regex("\\w+, \\w+ \\d{1,2}, \\d{4}"))

        // Wait for the GPS to be found
        hasText(R.id.pip_button, string(R.string.pip_button))

        // Update time from GPS
        click(toolbarButton(R.id.clock_title, Side.Right))

        // Click on the PIP button
        click(R.id.pip_button)

        handleExactAlarmsDialog()

        clickOk()

        // Verify the system time settings are opened
        // Check if the settings app is opened
        waitFor {
            view(By.textContains("time"))
//            viewWithText(Pattern.compile("time", Pattern.CASE_INSENSITIVE))
        }
    }
}