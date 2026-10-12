package com.kylecorry.trail_sense.tools.battery

import android.os.SystemClock
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.backUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.optional
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.tools.battery.domain.BatteryReading
import com.kylecorry.trail_sense.tools.battery.infrastructure.persistence.BatteryRepo
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.time.Duration
import java.time.Instant

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
    fun verifyBasicFunctionality() {
        hasText(Regex("\\d+%"))
        hasText(Regex("(Charging|Discharging).*"))
        hasText("Battery log")
        hasText("Updates every 1h - Low battery usage")
        verifyDeepSleep()

        isNotChecked(R.id.low_power_mode_switch)
        click(R.id.low_power_mode_switch)
        isChecked(R.id.low_power_mode_switch)

        // Stop the battery log
        click(com.kylecorry.andromeda.views.R.id.trailing_icon_btn)
        not { hasText("Battery log") }

        scrollUntil {
            click("Airplane mode")
        }
        optional {
            hasText("Network")
        }
        backUntil { isVisible(R.id.battery_title) }

        hasListItems(
            "Airplane mode",
            "Wi-Fi",
            "Bluetooth",
            "NFC",
            "Location",
            "Battery saver",
            "Data saver",
            "Adaptive brightness",
            "Screen timeout",
            "Dark theme",
            "Restrict app background activity",
            "Other tips"
        )

        // Open battery settings
        click(toolbarButton(R.id.battery_title, Side.Right))
        hasText("Battery")
    }

    private fun verifyDeepSleep() {
        if (isStagingBuild) {
            return
        }
        // The device stays awake while the test runs, so the percent drops slightly below 75%
        hasText(R.id.battery_deep_sleep, Regex("Deep sleep: 7[45]% \\(last 1h\\)"))
    }

    private fun hasListItems(vararg items: String) {
        for (item in items) {
            scrollUntil {
                hasText(item)
            }
        }
    }
}
