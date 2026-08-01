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
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import org.junit.Test
import java.time.Instant


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

        clearsSleepTimerUiWhenTimerExpires()

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

    @Test
    fun clearsSleepTimerUiWhenTimerExpires() {
        val preferences = PreferencesSubsystem.getInstance(TestUtils.context).preferences

        // Enable the sleep timer switch
        click(R.id.sleep_timer_switch)

        AutomationLibrary.isChecked(R.id.sleep_timer_switch)
        AutomationLibrary.isVisible(R.id.sleep_timer_picker)

        // Simulate a timer being set and then expiring:
        // first write a future deadline so the per-cycle effect sees an active timer...
        preferences.putInstant(WhiteNoiseService.CACHE_KEY_OFF_TIME, Instant.now().plusSeconds(60))
        // ...then remove it, just as WhiteNoiseService.onDestroy() does after natural expiry.
        preferences.remove(WhiteNoiseService.CACHE_KEY_OFF_TIME)

        // The UI should now reflect "no timer" within the next render cycle.
        isNotChecked(R.id.sleep_timer_switch)
        isNotVisible(R.id.sleep_timer_picker)
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
