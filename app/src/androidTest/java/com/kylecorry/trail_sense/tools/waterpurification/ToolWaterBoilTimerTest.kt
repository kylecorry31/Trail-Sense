package com.kylecorry.trail_sense.tools.waterpurification

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.notifications.hasTitle
import com.kylecorry.trail_sense.test_utils.notifications.notification
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.waterpurification.infrastructure.WaterPurificationTimerService
import org.junit.Test

class ToolWaterBoilTimerTest : ToolTestBase(Tools.WATER_BOIL_TIMER) {

    @Test
    fun verifyBasicFunctionality() {
        // Auto
        hasText(R.id.chip_auto, string(R.string.auto))
        hasText(R.id.time_left, waitForTime = 12000) { it == "180" || it == "60" }

        // Select 3 minutes
        hasText(R.id.chip_3_min, "3m")
        click(R.id.chip_3_min)
        hasText(R.id.time_left, "180")

        // Select 1 minute
        hasText(R.id.chip_1_min, "1m")
        click(R.id.chip_1_min)
        hasText(R.id.time_left, "60")

        // Start the timer
        click(R.id.boil_button)

        // Verify it is started
        hasText(R.id.boil_button, string(android.R.string.cancel))
        hasText(R.id.time_left) { it.toInt() <= 60 }
        notification(WaterPurificationTimerService.NOTIFICATION_ID).hasTitle(R.string.water_boil_timer_title)

        // TODO: Wait for the timer to finish and verify the finished state (simulate time passing)

        // Cancel the timer
        click(R.id.boil_button)

        // Verify it is stopped
        hasText(R.id.boil_button, string(R.string.start))
        hasText(R.id.time_left, "60")
        not { notification(WaterPurificationTimerService.NOTIFICATION_ID) }
    }
}