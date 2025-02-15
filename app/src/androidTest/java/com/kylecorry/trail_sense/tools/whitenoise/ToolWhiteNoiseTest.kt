package com.kylecorry.trail_sense.tools.whitenoise

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isFalse
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

        waitFor {
            notification(WhiteNoiseService.NOTIFICATION_ID)
                .hasTitle(R.string.tool_white_noise_title)
        }

        isTrue {
            TestUtils.isPlayingMusic()
        }


        // Turn it off
        click(R.id.white_noise_btn)

        not { notification(WhiteNoiseService.NOTIFICATION_ID) }
        isFalse {
            TestUtils.isPlayingMusic()
        }

        // TODO: The UIAutomator can't enter text in the duration input
//        canSetSleepTimer()

        verifyQuickAction()
    }

    private fun canSetSleepTimer() {
        click(R.id.sleep_timer_switch)
        input(R.id.duration, "2")

        // Turn on white noise
        click(R.id.white_noise_btn)

        waitFor {
            notification(WhiteNoiseService.NOTIFICATION_ID)
                .hasTitle(R.string.tool_white_noise_title)
        }

        isTrue {
            TestUtils.isPlayingMusic()
        }


        // Wait for the sleep timer to turn off the white noise
        not { notification(WhiteNoiseService.NOTIFICATION_ID) }
        isFalse { TestUtils.isPlayingMusic() }
    }

    private fun verifyQuickAction() {
        TestUtils.openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_WHITE_NOISE))

        waitFor {
            notification(WhiteNoiseService.NOTIFICATION_ID)
                .hasTitle(R.string.tool_white_noise_title)
        }

        isTrue {
            TestUtils.isPlayingMusic()
        }


        click(quickAction(Tools.QUICK_ACTION_WHITE_NOISE))

        not { notification(WhiteNoiseService.NOTIFICATION_ID) }

        isFalse { TestUtils.isPlayingMusic() }

        TestUtils.closeQuickActions()
    }
}