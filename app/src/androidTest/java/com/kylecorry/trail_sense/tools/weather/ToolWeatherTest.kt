package com.kylecorry.trail_sense.tools.weather

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.test_utils.AutomationLibrary
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.doesNotHaveNotification
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasNotification
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.optional
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollToStart
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestData
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherMonitorService
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.time.Duration

class ToolWeatherTest : ToolTestBase(Tools.WEATHER) {

    @Test
    fun verifyBasicFunctionality() {
        if (!Tools.isToolAvailable(context, Tools.WEATHER)) {
            return
        }

        // Historic temperature disclaimer
        clickOk()

        not(waitForTime = AutomationLibrary.GPS_WAIT_FOR_TIMEOUT) {
            hasText("Updating weather")
        }

        // Weather prediction
        hasText(R.id.weather_title) {
            it == string(R.string.weather_no_change) ||
                    it == string(R.string.weather_overcast) ||
                    it == string(R.string.weather_clear)
        }

        // Wait for a pressure reading
        scrollUntil { hasText(string(R.string.pressure)) }
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

        // Last reading
        scrollUntil { hasText(string(R.string.last_updated)) }
        scrollToStart()

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
        hasText(R.id.play_bar_title, "Off - 30m")
        click(R.id.play_btn)

        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            hasNotification(
                WeatherMonitorService.WEATHER_NOTIFICATION_ID,
                title = string(R.string.weather)
            )
        }

        // Wait for the battery restriction warning to go away
        optional {
            hasText(string(R.string.battery_settings_limit_accuracy))
            not { hasText(string(R.string.battery_settings_limit_accuracy)) }
        }

        hasText(R.id.play_bar_title, "On - 30m")

        click(R.id.play_btn)

        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            doesNotHaveNotification(WeatherMonitorService.WEATHER_NOTIFICATION_ID)
        }
    }

    private fun verifyQuickAction() {
        TestUtils.openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_WEATHER_MONITOR))

        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            hasNotification(
                WeatherMonitorService.WEATHER_NOTIFICATION_ID,
                title = string(R.string.weather)
            )
        }

        // Wait for the battery restriction warning to go away
        optional {
            hasText(string(R.string.battery_settings_limit_accuracy))
            not { hasText(string(R.string.battery_settings_limit_accuracy)) }
        }

        click(quickAction(Tools.QUICK_ACTION_WEATHER_MONITOR))

        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            doesNotHaveNotification(WeatherMonitorService.WEATHER_NOTIFICATION_ID)
        }

        TestUtils.closeQuickActions()
    }

    // Seeds known pressure histories and verifies the forecast shown to the user, covering the whole
    // pipeline from stored readings to the weather screen.

    @Test
    fun steadyPressureHasNoForecastedChange() {
        openWithPressureTrend(1020f, 1020f, Duration.ofHours(6))

        hasText(R.id.weather_title, string(R.string.weather_no_change))
        hasText("0.00 hPa / 3h", exact = true)
        hasText("1020.0 hPa", exact = true)
        not { hasText(string(R.string.alerts), exact = true, waitForTime = 0) }
    }

    @Test
    fun fallingPressureForecastsRain() {
        openWithPressureTrend(1018f, 1012f, Duration.ofHours(6))

        hasText(R.id.weather_title, string(R.string.precipitation_rain))
        hasText(R.id.weather_title, Regex(".*" + string(R.string.then_weather, "overcast")))
        // 6 hPa lost over 6 hours is a 3 hPa drop every 3 hours
        hasText("-3.00 hPa / 3h", exact = true)
        hasText("1012.0 hPa", exact = true)
        // A moderate drop is not a storm
        not { hasText(string(R.string.alerts), exact = true, waitForTime = 0) }
    }

    @Test
    fun rapidlyFallingPressureShowsAStormAlert() {
        openWithPressureTrend(1020f, 1010f, Duration.ofHours(3))

        hasText(R.id.weather_title, string(R.string.precipitation_rain))
        hasText(R.id.weather_title, string(R.string.very_soon).lowercase())
        hasText("-10.00 hPa / 3h", exact = true)
        hasText("1010.0 hPa", exact = true)

        hasText(string(R.string.alerts), exact = true)
        hasText(string(R.string.weather_storm), exact = true)
        click(string(R.string.alerts), exact = true)
        hasText(string(R.string.weather_alert_storm_description))
        clickOk()
    }

    @Test
    fun risingPressureForecastsClearingSkies() {
        openWithPressureTrend(1008f, 1016f, Duration.ofHours(6))

        hasText(R.id.weather_title, string(R.string.weather_wind))
        hasText(R.id.weather_title, Regex(".*" + string(R.string.then_weather, "clear")))
        hasText("4.00 hPa / 3h", exact = true)
        hasText("1016.0 hPa", exact = true)
        not { hasText(string(R.string.alerts), exact = true, waitForTime = 0) }
    }

    @Test
    fun readingsSpanningUnderTenMinutesAreNotUsedToForecast() {
        // The same large drop, but in too short a period to be trusted
        openWithPressureTrend(
            1020f,
            1010f,
            Duration.ofMinutes(5),
            interval = Duration.ofMinutes(1)
        )

        hasText(R.id.weather_title, string(R.string.weather_no_change))
        not { hasText(string(R.string.alerts), exact = true, waitForTime = 0) }
    }

    @Test
    fun pressureIsConvertedToTheSelectedUnits() {
        openWithPressureTrend(1013.25f, 1013.25f, Duration.ofHours(6), units = "in")

        hasText(Regex("29\\.92 in"))
    }

    @Test
    fun pressureHistoryIsCharted() {
        openWithPressureTrend(1018f, 1012f, Duration.ofHours(6))

        isVisible(R.id.chart)
    }

    private fun setPressureUnits(units: String) {
        PreferencesSubsystem.getInstance(TestUtils.context).preferences.putString(
            string(R.string.pref_pressure_units),
            units
        )
    }

    private fun openWithPressureTrend(
        start: Float,
        end: Float,
        duration: Duration,
        interval: Duration = Duration.ofMinutes(15),
        units: String = "hpa"
    ) {
        assumeFalse(isStagingBuild)
        assumeTrue(Tools.isToolAvailable(TestUtils.context, Tools.WEATHER))
        setPressureUnits(units)
        relaunchTool {
            TestData.addPressureTrend(start, end, duration, interval = interval)
        }
        optional { clickOk(waitForTime = 1000) }
    }
}
