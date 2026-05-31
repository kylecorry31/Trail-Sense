package com.kylecorry.trail_sense.tools.whitenoise

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isFalse
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isTrue
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.test_utils.views.isChecked
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.WhiteNoiseService
import org.junit.Test


class ToolWhiteNoiseTest : ToolTestBase(Tools.WHITE_NOISE) {

    @Test
    fun verifyBasicFunctionality() {
        // Turn on white noise
        clickTile(R.id.white_noise_btn)

        isTrue {
            WhiteNoiseService.isRunning
        }
        isAudioOutputActive()
        isNotificationVisible()
        isTileChecked(R.id.white_noise_btn)

        // Turn it off
        clickTile(R.id.white_noise_btn)

        isFalse {
            WhiteNoiseService.isRunning
        }
        isNotificationVisible(false)
        isTileChecked(R.id.white_noise_btn, false)

        // TODO: The UIAutomator can't enter text in the duration input
//        canSetSleepTimer()

        canChangeSleepSound()

        verifyQuickAction()
    }

    private fun canChangeSleepSound(){
        click("Pink noise")
        click("Crickets")
        clickOk()
        hasText("Crickets")
    }

    private fun canSetSleepTimer() {
        click(R.id.sleep_timer_switch)
        input(R.id.duration, "2")

        // Turn on white noise
        clickTile(R.id.white_noise_btn)

        isTrue {
            WhiteNoiseService.isRunning
        }
        isAudioOutputActive()

        // Wait for the sleep timer to turn off the white noise
        isFalse { WhiteNoiseService.isRunning }
    }

    private fun verifyQuickAction() {
        TestUtils.openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_WHITE_NOISE))

        isTrue {
            WhiteNoiseService.isRunning
        }
        isAudioOutputActive()
        isNotificationVisible()

        click(quickAction(Tools.QUICK_ACTION_WHITE_NOISE))

        isFalse { WhiteNoiseService.isRunning }
        isNotificationVisible(false)

        TestUtils.closeQuickActions()
    }

    private fun clickTile(id: Int) {
        click(id, childId = R.id.tile_btn)
    }

    private fun isAudioOutputActive() {
        isTrue {
            TestUtils.isAudioOutputActive()
        }
    }

    private fun isNotificationVisible(isVisible: Boolean = true) {
        val packageName = AutomationLibrary.packageName ?: TestUtils.context.packageName
        if (isVisible) {
            isTrue {
                TestUtils.hasNotification(WhiteNoiseService.NOTIFICATION_ID, packageName)
            }
        } else {
            isFalse {
                TestUtils.hasNotification(WhiteNoiseService.NOTIFICATION_ID, packageName)
            }
        }
    }

    private fun isTileChecked(id: Int, isChecked: Boolean = true) {
        waitFor {
            view(id, childId = R.id.tile_btn).isChecked(isChecked)
        }
    }
}
