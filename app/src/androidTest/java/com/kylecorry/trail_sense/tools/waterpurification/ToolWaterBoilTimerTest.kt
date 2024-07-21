package com.kylecorry.trail_sense.tools.waterpurification

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.not
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.notifications.hasTitle
import com.kylecorry.trail_sense.test_utils.notifications.notification
import com.kylecorry.trail_sense.test_utils.views.click
import com.kylecorry.trail_sense.test_utils.views.hasText
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.waterpurification.infrastructure.WaterPurificationTimerService
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ToolWaterBoilTimerTest {

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
        TestUtils.startWithTool(Tools.WATER_BOIL_TIMER)
    }

    @Test
    fun basicFunctionality() {
        // Auto
        // TODO: Mock out elevation
        waitFor(12500) {
            view(R.id.time_left).hasText { it == "180" || it == "60" }
        }

        // Select 3 minutes
        view(R.id.chip_3_min).click()
        view(R.id.time_left).hasText("180")

        // Select 1 minute
        view(R.id.chip_1_min).click()
        view(R.id.time_left).hasText("60")

        // Start the timer
        view(R.id.boil_button).click()

        // Verify it is started
        view(R.id.boil_button).hasText(android.R.string.cancel)
        view(R.id.time_left).hasText { it.toInt() <= 60 }
        notification(WaterPurificationTimerService.NOTIFICATION_ID).hasTitle(R.string.water_boil_timer_title)

        // TODO: Wait for the timer to finish and verify the finished state (simulate time passing)

        // Cancel the timer
        view(R.id.boil_button).click()

        // Verify it is stopped
        view(R.id.boil_button).hasText(R.string.start)
        view(R.id.time_left).hasText("60")
        not { notification(WaterPurificationTimerService.NOTIFICATION_ID) }
    }
}