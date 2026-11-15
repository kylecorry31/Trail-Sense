package com.kylecorry.trail_sense.tools.battery

import android.os.SystemClock
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.OPTIONAL_WAIT_FOR_TIMEOUT
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.backUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickRightToolbarButton
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.doesNotHaveText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.optional
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.ListItem
import com.kylecorry.trail_sense.tools.battery.domain.BatteryReading
import com.kylecorry.trail_sense.tools.battery.infrastructure.persistence.BatteryRepo
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeFalse
import org.junit.Test

class ToolBatteryTest : ToolTestBase(Tools.BATTERY) {

    // The device was awake for 15 of the last 60 minutes
    override fun seedData() = runBlocking {
        val elapsed = Duration.ofHours(1)
        BatteryRepo.getInstance(context).add(
            BatteryReading(
                Instant.now().minus(elapsed),
                100f,
                0f,
                false,
                Duration.ofMillis(SystemClock.uptimeMillis()).minusMinutes(15),
                Duration.ofMillis(SystemClock.elapsedRealtime()).minus(elapsed)
            )
        )
    }

    @Test
    fun showsTheBatteryChargeAndTheBatteryLog() {
        // The charge comes from the emulator's battery, so only the format is checked
        hasText(Regex("\\d+%"))
        hasText(Regex("(Charging|Discharging).*"))
        hasText("Battery log")
        hasText("Updates every 1h - Low battery usage")
    }

    @Test
    fun showsTheDeepSleepFromTheBatteryLog() {
        assumeFalse(isStagingBuild)

        // The device stays awake while the test runs, so the percent drops slightly below 75%
        hasText(R.id.battery_deep_sleep, Regex("Deep sleep: 7[45]% \\(last 1h\\)"))
    }

    @Test
    fun canTurnOnLowPowerModeAndStopTheBatteryLog() {
        isNotChecked(R.id.low_power_mode_switch)
        click(R.id.low_power_mode_switch)
        isChecked(R.id.low_power_mode_switch)

        // Stop the battery log
        click(ListItem.trailingIconButton)
        doesNotHaveText("Battery log")
    }

    @Test
    fun showsTheSystemBatteryTipsAndOpensTheBatterySettings() {
        scrollUntil {
            click("Airplane mode")
        }
        optional(OPTIONAL_WAIT_FOR_TIMEOUT) {
            hasText("Network")
        }
        backUntil { isVisible(R.id.battery_title) }

        hasListItems("Airplane mode", "Other tips")

        // Open battery settings
        clickRightToolbarButton(R.id.battery_title)
        hasText("Battery")
    }

    private fun hasListItems(vararg items: String) {
        for (item in items) {
            scrollUntil(maxScrolls = 30) {
                hasText(item)
            }
        }
    }
}
