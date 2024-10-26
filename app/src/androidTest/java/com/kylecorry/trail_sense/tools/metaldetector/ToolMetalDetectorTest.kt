package com.kylecorry.trail_sense.tools.metaldetector

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ActivityScenario
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.mute
import com.kylecorry.trail_sense.test_utils.TestUtils.unmute
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.click
import com.kylecorry.trail_sense.test_utils.views.hasText
import com.kylecorry.trail_sense.test_utils.views.isChecked
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ToolMetalDetectorTest {

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
        scenario = TestUtils.startWithTool(Tools.METAL_DETECTOR)
    }

    @Test
    fun verifyBasicFunctionality() {
        // Verify the title
        waitFor {
            view(R.id.metal_detector_title).hasText(Regex("\\d+ uT"))
        }

        // Make sure the chart is visible
        view(R.id.metal_chart)

        // Can turn on high sensitivity
        view(R.id.high_sensitivity_toggle)
            .isChecked(false)
            .click()
            .isChecked()

        // Can adjust threshold (just verify it is set)
        view(R.id.threshold)
        view(R.id.threshold_amount).hasText("5.0 uT")

        // Can play sound
        val originalVolume = mute()
        toolbarButton(R.id.metal_detector_title, Side.Right)
            .click()
            .click()
        unmute(originalVolume)
    }
}