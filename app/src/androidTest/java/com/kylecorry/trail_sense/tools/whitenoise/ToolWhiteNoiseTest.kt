package com.kylecorry.trail_sense.tools.whitenoise

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isFalse
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isTrue
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.notifications.hasTitle
import com.kylecorry.trail_sense.test_utils.notifications.notification
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.WhiteNoiseService
import org.junit.Test


class ToolWhiteNoiseTest : ToolTestBase(Tools.WHITE_NOISE) {

    @Test
    fun verifyBasicFunctionality() {
        // Turn on white noise
        click(R.id.white_noise_btn)

        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            waitFor {
                notification(WhiteNoiseService.NOTIFICATION_ID)
                    .hasTitle(R.string.tool_white_noise_title)
            }
        }

        isTrue {
            TestUtils.isPlayingMusic()
        }


        // Turn it off
        click(R.id.white_noise_btn)

        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            not { notification(WhiteNoiseService.NOTIFICATION_ID) }
        }
        isFalse {
            TestUtils.isPlayingMusic()
        }

        // TODO: The UIAutomator can't enter text in the duration input
//        canSetSleepTimer()

        canChangeSleepSound()

        sleepTimerUiClearsAfterPlaybackStops()

        verifyQuickAction()
    }

    private fun canChangeSleepSound() {
        click("Pink noise")
        click("Crickets")
        clickOk()
        hasText("Crickets")
    }

    private fun canSetSleepTimer() {
        click(R.id.sleep_timer_switch)
        input(R.id.duration, "2")

        // Turn on white noise
        click(R.id.white_noise_btn)

        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            waitFor {
                notification(WhiteNoiseService.NOTIFICATION_ID)
                    .hasTitle(R.string.tool_white_noise_title)
            }
        }

        isTrue {
            TestUtils.isPlayingMusic()
        }


        // Wait for the sleep timer to turn off the white noise
        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            not { notification(WhiteNoiseService.NOTIFICATION_ID) }
        }
        isFalse { TestUtils.isPlayingMusic() }
    }

    /**
     * Verifies that when playback stops (naturally or manually while the sleep timer switch is on),
     * the sleep timer switch is unchecked and the picker is hidden.
     *
     * Regression test for https://github.com/kylecorry31/Trail-Sense/issues/3926
     */
    private fun sleepTimerUiClearsAfterPlaybackStops() {
        // Enable the sleep timer and start playback
        click(R.id.sleep_timer_switch)
        click(R.id.white_noise_btn)

        isTrue { TestUtils.isPlayingMusic() }

        // Stop playback manually (simulates what the timer expiry also does)
        click(R.id.white_noise_btn)

        isFalse { TestUtils.isPlayingMusic() }

        // The sleep timer UI should reset: switch unchecked, picker hidden
        waitFor {
            isNotChecked(R.id.sleep_timer_switch)
            isNotVisible(R.id.sleep_timer_picker)
        }
    }

    private fun verifyQuickAction() {
        TestUtils.openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_WHITE_NOISE))

        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            waitFor {
                notification(WhiteNoiseService.NOTIFICATION_ID)
                    .hasTitle(R.string.tool_white_noise_title)
            }
        }

        isTrue {
            TestUtils.isPlayingMusic()
        }


        click(quickAction(Tools.QUICK_ACTION_WHITE_NOISE))

        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            not { notification(WhiteNoiseService.NOTIFICATION_ID) }
        }

        isFalse { TestUtils.isPlayingMusic() }

        TestUtils.closeQuickActions()
    }
}