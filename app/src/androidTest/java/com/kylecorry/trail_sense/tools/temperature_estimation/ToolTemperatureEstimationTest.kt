package com.kylecorry.trail_sense.tools.temperature_estimation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kylecorry.luna.text.toFloatCompat
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.views.click
import com.kylecorry.trail_sense.test_utils.views.hasText
import com.kylecorry.trail_sense.test_utils.views.input
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
        TestUtils.setWaitForIdleTimeout(100)
        TestUtils.setupApplication()
        TestUtils.startWithTool(Tools.TEMPERATURE_ESTIMATION)
    }

    @Test
    fun verifyBasicFunctionality() {
        waitFor {
            view(R.id.temperature_title)
        }

        view(R.id.temp_est_base_elevation, R.id.amount).input("100")
        view(R.id.temp_est_dest_elevation, R.id.amount).input("1000")
        view(R.id.temp_est_base_temperature, R.id.amount).input("15")

        view(R.id.temperature_title).hasText("12 °F")

        // Clear all views
        view(R.id.temp_est_base_elevation, R.id.amount).input("")
        view(R.id.temp_est_base_temperature, R.id.amount).input("")

        // Autofill
        view(R.id.temp_est_autofill).click()

        waitFor(12000) {
            view(R.id.temp_est_base_elevation, R.id.amount).hasText {
                it.split(",").first().toFloatCompat() != null
            }
            view(R.id.temp_est_base_temperature, R.id.amount).hasText {
                it.split(",").first().toFloatCompat() != null
            }
        }
    }
}