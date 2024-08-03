package com.kylecorry.trail_sense.tools.climate

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ActivityScenario
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.pickDate
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
class ToolClimateTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    val grantPermissionRule = TestUtils.allPermissionsGranted()

    @get:Rule
    val instantExec = InstantTaskExecutorRule()

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        hiltRule.inject()
        TestUtils.setupApplication()
        TestUtils.setWaitForIdleTimeout(100)
        scenario = TestUtils.startWithTool(Tools.CLIMATE)
    }

    @Test
    fun verifyBasicFunctionality() {
        // Verify the title
        waitFor {
            view(R.id.climate_title).hasText(R.string.temperature_high_low)
        }

        // Verify today is selected by default
        view(R.id.display_date).hasText(R.string.today)

        view(R.id.utm).input("42, -72")
        view(R.id.elevation_input, R.id.amount).input("1000")

        // Change the date
        view(R.id.date_btn).click()
        pickDate(2024, 8, 5)

        // Verify the temperature is displayed
        waitFor {
            view(R.id.climate_title).hasText("81 °F / 59 °F")
        }
    }
}