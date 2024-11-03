package com.kylecorry.trail_sense.tools.light

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ToolLightMeterTest {

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
        TestUtils.setWaitForIdleTimeout()
        TestUtils.startWithTool(Tools.LIGHT_METER)
    }

    @Test
    fun verifyBasicFunctionality() {
        hasText(R.id.light_title, Regex("\\d+ lx"))

        // Enter the distance
        input(R.id.amount, "10")

        // The candella and beam distance should be shown
        hasText(R.id.light_title, Regex("\\d+ cd"))
        hasText(R.id.beam_distance_text, Regex("\\d+ ft beam distance"))

        // Reset
        click(R.id.reset_btn)
        hasText(R.id.light_title, Regex("\\d+ cd"))
        hasText(R.id.beam_distance_text, Regex("\\d+ ft beam distance"))
    }
}