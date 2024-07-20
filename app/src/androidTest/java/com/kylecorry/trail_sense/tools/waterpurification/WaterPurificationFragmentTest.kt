package com.kylecorry.trail_sense.tools.waterpurification

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.waterpurification.infrastructure.WaterPurificationTimerService
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class WaterPurificationFragmentTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    val grantPermissionRule = TestUtils.mainPermissionsGranted()

    @get:Rule
    val instantExec = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        hiltRule.inject()
        TestUtils.setupDefaultPreferences()
        TestUtils.startWithTool(Tools.WATER_BOIL_TIMER)
    }

    @Test
    fun basicFunctionality() {
        // Auto
        // TODO: Mock out elevation
        TestUtils.hasText(R.id.time_left, "180")

        // Select 3 minutes
        TestUtils.click(R.id.chip_3_min)
        TestUtils.hasText(R.id.time_left, "180")

        // Select 1 minute
        TestUtils.click(R.id.chip_1_min)
        TestUtils.hasText(R.id.time_left, "60")

        // Start the timer
        TestUtils.click(R.id.boil_button)

        // Verify it is started
        TestUtils.hasText(R.id.boil_button, android.R.string.cancel)
        TestUtils.hasText(R.id.time_left) { it.toInt() <= 60 }
        TestUtils.hasNotification(WaterPurificationTimerService.NOTIFICATION_ID)

        // TODO: Wait for the timer to finish and verify the finished state (simulate time passing)

        // Cancel the timer
        TestUtils.click(R.id.boil_button)

        // Verify it is stopped
        TestUtils.hasText(R.id.boil_button, R.string.start)
        TestUtils.hasText(R.id.time_left, "60")
        TestUtils.doesNotHaveNotification(WaterPurificationTimerService.NOTIFICATION_ID)
    }
}