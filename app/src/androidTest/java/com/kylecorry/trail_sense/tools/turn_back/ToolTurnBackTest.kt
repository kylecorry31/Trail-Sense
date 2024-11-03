package com.kylecorry.trail_sense.tools.turn_back

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.handleExactAlarmsDialog
import com.kylecorry.trail_sense.test_utils.TestUtils.pickTime
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.views.click
import com.kylecorry.trail_sense.test_utils.views.hasText
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ToolTurnBackTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    val grantPermissionRule = TestUtils.mainPermissionsGranted()

    @get:Rule
    val instantExec = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        hiltRule.inject()
        TestUtils.setupApplication()
        TestUtils.setWaitForIdleTimeout(100)
        TestUtils.startWithTool(Tools.TURN_BACK)
    }

    @Test
    fun verifyBasicFunctionality() {
        waitFor {
            view(R.id.instructions).hasText(R.string.time_not_set)
        }

        // Enter a time
        view(R.id.edittext).click()
        pickTime(8, 0, false)

        handleExactAlarmsDialog()

        waitFor {
            view(R.id.edittext).hasText("8:00 PM", contains = true)
            view(R.id.instructions).hasText(Regex("Turn around by \\d+:\\d+ (AM|PM) \\(.*\\) to return at 8:00 PM"))
        }

        // Cancel
        view(R.id.cancel_button).click()

        waitFor {
            view(R.id.instructions).hasText(R.string.time_not_set)
        }

        // Return before dark
        view(R.id.sunset_button).click()

        // Verify a time is set
        waitFor(12000) {
            handleExactAlarmsDialog()
            view(R.id.edittext).hasText(Regex("\\d+:\\d+ (AM|PM).*"))
        }
    }
}