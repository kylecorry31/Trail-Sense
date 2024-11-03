package com.kylecorry.trail_sense.tools.temperature_estimation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kylecorry.luna.text.toFloatCompat
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ToolTemperatureEstimationTest {

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
        TestUtils.startWithTool(Tools.TEMPERATURE_ESTIMATION)
    }

    @Test
    fun verifyBasicFunctionality() {
        waitFor {
            view(R.id.temperature_title)
        }

        input(R.id.temp_est_base_elevation, "100")
        input(R.id.temp_est_dest_elevation, "1000")
        input(R.id.temp_est_base_temperature, "15")

        hasText(R.id.temperature_title, "12 °F")

        // Clear all views
        input(R.id.temp_est_base_elevation, "")
        input(R.id.temp_est_base_temperature, "")

        // Autofill
        click(R.id.temp_est_autofill)

        hasText(R.id.temp_est_base_elevation, waitForTime = 12000) {
            it.split(",").first().toFloatCompat() != null
        }
        hasText(R.id.temp_est_base_temperature) {
            it.split(",").first().toFloatCompat() != null
        }
    }
}