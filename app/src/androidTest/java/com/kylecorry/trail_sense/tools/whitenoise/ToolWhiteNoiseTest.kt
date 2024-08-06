package com.kylecorry.trail_sense.tools.whitenoise

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.not
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.notifications.hasTitle
import com.kylecorry.trail_sense.test_utils.notifications.notification
import com.kylecorry.trail_sense.test_utils.views.click
import com.kylecorry.trail_sense.test_utils.views.input
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.test_utils.views.viewWithText
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.WhiteNoiseService
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test


@HiltAndroidTest
class ToolWhiteNoiseTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    val grantPermissionRule = TestUtils.mainPermissionsGranted()

    @get:Rule
    val instantExec = InstantTaskExecutorRule()

    private var currentVolume = 0

    @Before
    fun setUp() {
        hiltRule.inject()
        currentVolume = TestUtils.mute()
        TestUtils.setWaitForIdleTimeout(100)
        TestUtils.setupApplication()
        TestUtils.startWithTool(Tools.WHITE_NOISE)

        // Wait for the tool to open
        waitFor {
            view(R.id.white_noise_btn)
        }
    }

    @Test
    fun verifyBasicFunctionality() {
        // Turn on white noise
        view(R.id.white_noise_btn).click()

        waitFor {
            notification(WhiteNoiseService.NOTIFICATION_ID)
                .hasTitle(R.string.tool_white_noise_title)
            assertTrue(TestUtils.isPlayingMusic())
        }

        // Turn it off
        view(R.id.white_noise_btn).click()

        waitFor {
            not { notification(WhiteNoiseService.NOTIFICATION_ID) }
            assertFalse(TestUtils.isPlayingMusic())
        }

        // TODO: The UIAutomator can't enter text in the duration input
//        canSetSleepTimer()

        verifyQuickAction()
    }

    private fun canSetSleepTimer() {
        view(R.id.sleep_timer_switch).click()
        view(R.id.duration).input("2")

        // Turn on white noise
        view(R.id.white_noise_btn).click()

        waitFor {
            notification(WhiteNoiseService.NOTIFICATION_ID)
                .hasTitle(R.string.tool_white_noise_title)
            assertTrue(TestUtils.isPlayingMusic())
        }

        // Wait for the sleep timer to turn off the white noise
        waitFor {
            not { notification(WhiteNoiseService.NOTIFICATION_ID) }
            assertFalse(TestUtils.isPlayingMusic())
        }
    }

    private fun verifyQuickAction() {
        TestUtils.openQuickActions()
        quickAction(Tools.QUICK_ACTION_WHITE_NOISE).click()

        waitFor {
            notification(WhiteNoiseService.NOTIFICATION_ID)
                .hasTitle(R.string.tool_white_noise_title)
            assertTrue(TestUtils.isPlayingMusic())
        }

        quickAction(Tools.QUICK_ACTION_WHITE_NOISE).click()

        waitFor {
            not { notification(WhiteNoiseService.NOTIFICATION_ID) }
            assertFalse(TestUtils.isPlayingMusic())
        }

        TestUtils.closeQuickActions()
    }

    @After
    fun tearDown() {
        TestUtils.unmute(currentVolume)
    }
}