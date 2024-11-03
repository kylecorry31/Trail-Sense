package com.kylecorry.trail_sense.tools.solarpanel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.context
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
        TestUtils.setWaitForIdleTimeout()
        TestUtils.setupApplication()
        TestUtils.startWithTool(Tools.SOLAR_PANEL_ALIGNER)
    }

    @Test
    fun verifyBasicFunctionality() {
        if (!Tools.isToolAvailable(context, Tools.SOLAR_PANEL_ALIGNER)) {
            return
        }

        clickOk()

        // It should show the solar panel details for today
        hasText(R.id.tilt_label, string(R.string.tilt))
        hasText(R.id.current_altitude, Regex("\\d+°"))
        hasText(R.id.desired_altitude, Regex("\\d+°"))

        hasText(R.id.azimuth_label, string(R.string.compass_azimuth))
        hasText(R.id.current_azimuth, Regex("\\d+°"))
        hasText(R.id.desired_azimuth, Regex("\\d+°"))

        hasText(R.id.energy, Regex("Up to \\d+(\\.\\d+)? kWh / m²"))

        // TODO: Verify that today is selected

        // It should show the solar panel details for a custom time
        click(R.id.solar_now_btn)

        clickOk()

        hasText(R.id.tilt_label, string(R.string.tilt))
        hasText(R.id.current_altitude, Regex("\\d+°"))
        hasText(R.id.desired_altitude, Regex("\\d+°"))

        hasText(R.id.azimuth_label, string(R.string.compass_azimuth))
        hasText(R.id.current_azimuth, Regex("\\d+°"))
        hasText(R.id.desired_azimuth, Regex("\\d+°"))

        hasText(R.id.energy, Regex("Up to \\d+(\\.\\d+)? kWh / m²"))

        // TODO: Verify that the now button is selected
        hasText(R.id.solar_now_btn, "2h")
    }
}