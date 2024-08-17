package com.kylecorry.trail_sense.tools.solarpanel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.views.click
import com.kylecorry.trail_sense.test_utils.views.hasText
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.test_utils.views.viewWithText
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ToolSolarPanelAlignerTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    val grantPermissionRule = TestUtils.mainPermissionsGranted()

    @get:Rule
    val instantExec = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        hiltRule.inject()
        TestUtils.setWaitForIdleTimeout(100)
        TestUtils.setupApplication()
        TestUtils.startWithTool(Tools.SOLAR_PANEL_ALIGNER)
    }

    @Test
    fun verifyBasicFunctionality() {
        if (!Tools.isToolAvailable(context, Tools.SOLAR_PANEL_ALIGNER)) {
            return
        }

        waitFor {
            viewWithText(android.R.string.ok).click()
        }

        // It should show the solar panel details for today
        waitFor {
            view(R.id.tilt_label).hasText(R.string.tilt)
            view(R.id.current_altitude).hasText(Regex("\\d+°"))
            view(R.id.desired_altitude).hasText(Regex("\\d+°"))

            view(R.id.azimuth_label).hasText(R.string.compass_azimuth)
            view(R.id.current_azimuth).hasText(Regex("\\d+°"))
            view(R.id.desired_azimuth).hasText(Regex("\\d+°"))

            view(R.id.energy).hasText(Regex("Up to \\d+(\\.\\d+)? kWh / m²"))

            // TODO: Verify that today is selected
        }

        // It should show the solar panel details for a custom time
        view(R.id.solar_now_btn).click()

        waitFor {
            viewWithText(android.R.string.ok).click()
        }

        waitFor {
            view(R.id.tilt_label).hasText(R.string.tilt)
            view(R.id.current_altitude).hasText(Regex("\\d+°"))
            view(R.id.desired_altitude).hasText(Regex("\\d+°"))

            view(R.id.azimuth_label).hasText(R.string.compass_azimuth)
            view(R.id.current_azimuth).hasText(Regex("\\d+°"))
            view(R.id.desired_azimuth).hasText(Regex("\\d+°"))

            view(R.id.energy).hasText(Regex("Up to \\d+(\\.\\d+)? kWh / m²"))

            // TODO: Verify that the now button is selected
            view(R.id.solar_now_btn).hasText("2h")
        }
    }
}