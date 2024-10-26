package com.kylecorry.trail_sense.tools.clock

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ActivityScenario
import androidx.test.uiautomator.By
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.handleExactAlarmsDialog
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.click
import com.kylecorry.trail_sense.test_utils.views.hasText
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.test_utils.views.viewWithText
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.regex.Pattern

@HiltAndroidTest
class ToolClockTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    val grantPermissionRule = TestUtils.mainPermissionsGranted()

    @get:Rule
    val instantExec = InstantTaskExecutorRule()

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        hiltRule.inject()
        TestUtils.setWaitForIdleTimeout(100)
        TestUtils.setupApplication()
        scenario = TestUtils.startWithTool(Tools.CLOCK)
    }

    @Test
    fun verifyBasicFunctionality() {
        // Verify the title
        waitFor {
            view(R.id.clock_title).hasText(Regex("\\d{1,2}:\\d{2}:\\d{2} [AP]M"))
            view(R.id.clock_title).hasText(Regex("\\w+, \\w+ \\d{1,2}, \\d{4}"))
        }

        // Wait for the GPS to be found
        waitFor {
            view(R.id.pip_button).hasText(R.string.pip_button)
        }

        // Update time from GPS
        waitFor {
            toolbarButton(R.id.clock_title, Side.Right).click()
            view(R.id.updating_clock).hasText(R.string.clock_waiting_for_gps)
        }

        // Click on the PIP button
        waitFor {
            view(R.id.pip_button).click()
        }

        handleExactAlarmsDialog()

        waitFor {
            viewWithText(android.R.string.ok).click()
        }

        // Verify the system time settings are opened
        // Check if the settings app is opened
        waitFor {
            view(By.textContains("time"))
//            viewWithText(Pattern.compile("time", Pattern.CASE_INSENSITIVE))
        }
    }
}