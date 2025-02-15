package com.kylecorry.trail_sense.tools.flashlight

import com.kylecorry.andromeda.torch.Torch
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.doesNotHaveNotification
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasNotification
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isFalse
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isTrue
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.TestUtils.openQuickActions
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightService
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolFlashlightTest : ToolTestBase(Tools.FLASHLIGHT) {

    @Test
    fun verifyBasicFunctionality() {
        canToggleFlashlight()
        canToggleScreenFlashlight()
        verifyQuickAction()
    }

    private fun canToggleFlashlight() {
        if (!Torch.isAvailable(context)) {
            return
        }

        click(R.id.flashlight_on_btn)
        hasNotification(
            FlashlightService.NOTIFICATION_ID,
            title = string(R.string.flashlight_title)
        )
        isTrue { TestUtils.isTorchOn }

        click(R.id.flashlight_on_btn)
        doesNotHaveNotification(FlashlightService.NOTIFICATION_ID)
        isFalse { TestUtils.isTorchOn }
    }

    private fun canToggleScreenFlashlight() {
        click(R.id.screen_flashlight_btn)
        isNotVisible(R.id.screen_flashlight_btn)

        click(R.id.red_white_switcher)
        click(R.id.red_white_switcher)

        isVisible(R.id.brightness_seek)

        click(R.id.off_btn)

        isVisible(R.id.screen_flashlight_btn)
    }

    private fun verifyQuickAction() {
        openQuickActions()

        if (Torch.isAvailable(context)) {
            click(quickAction(Tools.QUICK_ACTION_FLASHLIGHT))
            hasNotification(
                FlashlightService.NOTIFICATION_ID,
                title = string(R.string.flashlight_title)
            )
            isTrue { TestUtils.isTorchOn }

            click(quickAction(Tools.QUICK_ACTION_FLASHLIGHT))
            doesNotHaveNotification(FlashlightService.NOTIFICATION_ID)
            isFalse { TestUtils.isTorchOn }
        }

        click(quickAction(Tools.QUICK_ACTION_SCREEN_FLASHLIGHT))
        isVisible(R.id.screen_flashlight)
    }

}