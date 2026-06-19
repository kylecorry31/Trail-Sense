package com.kylecorry.trail_sense.tools.pedometer

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.doesNotHaveNotification
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasNotification
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils.clickListItemMenu
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

class ToolPedometerTest : ToolTestBase(Tools.PEDOMETER) {

    @Test
    fun verifyBasicFunctionality() {
        if (!Tools.isToolAvailable(context, Tools.PEDOMETER)) {
            return
        }

        hasText(R.id.pedometer_title, string(R.string.pedometer))
        hasText(R.id.current_session_title, string(R.string.current_session))
        hasText(R.id.pedometer_steps, "0")
        hasText(R.id.pedometer_steps, string(R.string.steps))

        hasText(R.id.pedometer_distance, "0 ft")
        hasText(R.id.pedometer_distance, string(R.string.distance))

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

        hasText(R.id.current_session_time, Regex("\\d+:\\d+ [AP]M - ${string(R.string.now)}"))

        click(toolbarButton(R.id.pedometer_title, Side.Right))
        input(R.id.amount, "0")
        clickOk()

        click(R.id.play_btn)
        hasNotification(DistanceAlerter.NOTIFICATION_ID, title = string(R.string.distance_alert))

        click(R.id.play_btn)

        verifyHistory()

        verifyQuickAction()
    }

    private fun verifyHistory() {
        scrollUntil(R.id.pedometer_scroll) {
            hasText(R.id.history_title, string(R.string.history))
        }

        hasText(R.id.hourly_steps_date, string(R.string.today))
        hasText(R.id.hourly_steps_total_steps, "0")
        hasText(R.id.hourly_steps_total_steps, string(R.string.steps))
        hasText(R.id.hourly_steps_total_distance, "0 ft")
        hasText(R.id.hourly_steps_total_distance, string(R.string.distance))

        click(R.id.prev_date)
        hasText(R.id.hourly_steps_date, string(R.string.yesterday))
        click(R.id.next_date)
        hasText(R.id.hourly_steps_date, string(R.string.today))

        scrollUntil(R.id.pedometer_scroll) {
            isVisible(R.id.hourly_steps_chart)
        }

        click(R.id.pedometer_sessions_btn)
        hasText(string(R.string.history))
        hasText("0 steps")
        clickListItemMenu("Delete")
        clickOk()
        not { hasText("0 steps") }
        clickOk()
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
