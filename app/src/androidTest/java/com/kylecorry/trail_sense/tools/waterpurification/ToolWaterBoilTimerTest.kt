package com.kylecorry.trail_sense.tools.waterpurification

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.GPS_WAIT_FOR_TIMEOUT
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
    fun canSelectTheBoilDuration() {
        // Auto (3 minutes at high elevation, otherwise 1 minute, which depends on the emulator's location)
        hasText(R.id.chip_auto, string(R.string.auto))
        hasText(R.id.time_left, waitForTime = GPS_WAIT_FOR_TIMEOUT) { it == "180" || it == "60" }

        hasText(R.id.chip_3_min, "3m")
        click(R.id.chip_3_min)
        hasText(R.id.time_left, "180")

        hasText(R.id.chip_1_min, "1m")
        click(R.id.chip_1_min)
        hasText(R.id.time_left, "60")
    }

    @Test
    fun canStartAndCancelTheTimer() {
        click(R.id.chip_1_min)
        hasText(R.id.time_left, "60")

        click(R.id.boil_button)

        hasText(R.id.boil_button, string(android.R.string.cancel))
        hasText(R.id.time_left) { it.toInt() <= 60 }

        if (AutomationLibrary.packageName == null) {
            notification(WaterPurificationTimerService.NOTIFICATION_ID).hasTitle(R.string.water_boil_timer_title)
        }

        click(R.id.boil_button)

        hasText(R.id.boil_button, string(R.string.start))
        hasText(R.id.time_left, "60")

        if (AutomationLibrary.packageName == null) {
            not { notification(WaterPurificationTimerService.NOTIFICATION_ID) }
        }
    }
}
