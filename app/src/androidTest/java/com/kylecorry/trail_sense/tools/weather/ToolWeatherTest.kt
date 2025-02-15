package com.kylecorry.trail_sense.tools.weather

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.doesNotHaveNotification
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasNotification
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.optional
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherMonitorService
import org.junit.Test

class ToolWeatherTest : ToolTestBase(Tools.WEATHER) {

    @Test
    fun verifyBasicFunctionality() {
        if (!Tools.isToolAvailable(context, Tools.WEATHER)) {
            return
        }

        // Historic temperature disclaimer
        clickOk()

        // Weather prediction
        hasText(R.id.weather_title) {
            it == string(R.string.weather_no_change) ||
                    it == string(R.string.weather_overcast) ||
                    it == string(R.string.weather_clear)
        }

        // Wait for a pressure reading
        hasText(string(R.string.pressure))
        hasText(Regex("\\d+(\\.\\d+)? in"))

        // Historic temperature
        optional {
            hasText(string(R.string.temperature))
            hasText(string(R.string.historic_temperature_years, 30))
            hasText(Regex("\\d+(\\.\\d+)? °F"))
        }

        // High / low
        optional {
            hasText(string(R.string.temperature_high_low))
            hasText(Regex("\\d+(\\.\\d+)? °F / \\d+(\\.\\d+)? °F"))
        }

        // Pressure chart
        isVisible(R.id.chart)

        // High / low chart
        optional {
            click(string(R.string.temperature_high_low))
            hasText(string(R.string.next_24_hours))
            isVisible(R.id.chart)
            clickOk()
        }

        canUseWeatherMonitor()
        verifyQuickAction()
    }

    private fun canUseWeatherMonitor() {
        hasText(R.id.play_bar_title, "Off - 15m")
        click(R.id.play_btn)
        hasNotification(
            WeatherMonitorService.WEATHER_NOTIFICATION_ID,
            title = string(R.string.weather)
        )

        // Wait for the battery restriction warning to go away
        optional {
            hasText(string(R.string.battery_settings_limit_accuracy))
            not { hasText(string(R.string.battery_settings_limit_accuracy), waitForTime = 0) }
        }

        hasText(R.id.play_bar_title, "On - 15m")

        click(R.id.play_btn)

        doesNotHaveNotification(WeatherMonitorService.WEATHER_NOTIFICATION_ID)
    }

    private fun verifyQuickAction() {
        TestUtils.openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_WEATHER_MONITOR))

        hasNotification(
            WeatherMonitorService.WEATHER_NOTIFICATION_ID,
            title = string(R.string.weather)
        )

        // Wait for the battery restriction warning to go away
        optional {
            hasText(string(R.string.battery_settings_limit_accuracy))
            not { hasText(string(R.string.battery_settings_limit_accuracy), waitForTime = 0) }
        }

        click(quickAction(Tools.QUICK_ACTION_WEATHER_MONITOR))

        doesNotHaveNotification(WeatherMonitorService.WEATHER_NOTIFICATION_ID)

        TestUtils.closeQuickActions()
    }

}