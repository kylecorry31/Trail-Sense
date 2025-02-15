package com.kylecorry.trail_sense.tools.pedometer

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.doesNotHaveNotification
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasNotification
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils.closeQuickActions
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.TestUtils.openQuickActions
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.DistanceAlerter
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounterService
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test
import kotlin.text.Regex

class ToolPedometerTest : ToolTestBase(Tools.PEDOMETER) {

    @Test
    fun verifyBasicFunctionality() {
        if (!Tools.isToolAvailable(context, Tools.PEDOMETER)) {
            return
        }

        hasText(R.id.pedometer_title, "0 ft")
        hasText(R.id.pedometer_steps, "0")
        hasText(R.id.pedometer_steps, string(R.string.steps))

        hasText(R.id.pedometer_speed, "-")
        hasText(R.id.pedometer_speed, string(R.string.current_speed))

        hasText(R.id.pedometer_average_speed, "-")
        hasText(R.id.pedometer_average_speed, string(R.string.average_speed))

        hasText(R.id.play_bar_title, string(R.string.off))

        click(R.id.play_btn)
        hasText(R.id.play_bar_title, string(R.string.on))
        hasNotification(StepCounterService.NOTIFICATION_ID, title = string(R.string.pedometer))

        click(R.id.play_btn)
        doesNotHaveNotification(StepCounterService.NOTIFICATION_ID)
        hasText(R.id.play_bar_title, string(R.string.off))

        click(R.id.reset_btn)
        clickOk()

        hasText(R.id.pedometer_title, Regex("since \\d+:\\d+ [AP]M"))

        click(toolbarButton(R.id.pedometer_title, Side.Right))
        input(R.id.amount, "0")
        clickOk()

        click(R.id.play_btn)
        hasNotification(DistanceAlerter.NOTIFICATION_ID, title = string(R.string.distance_alert))

        click(R.id.play_btn)

        verifyQuickAction()
    }

    private fun verifyQuickAction() {
        openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_PEDOMETER))
        hasNotification(StepCounterService.NOTIFICATION_ID, title = string(R.string.pedometer))
        click(quickAction(Tools.QUICK_ACTION_PEDOMETER))
        doesNotHaveNotification(StepCounterService.NOTIFICATION_ID)
        closeQuickActions()
    }

}