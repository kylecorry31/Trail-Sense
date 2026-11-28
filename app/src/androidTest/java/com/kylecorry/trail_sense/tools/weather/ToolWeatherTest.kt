package com.kylecorry.trail_sense.tools.weather

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.test_utils.AutomationLibrary
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickQuickAction
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.doesNotHaveNotification
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.doesNotHaveText
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
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherMonitorService
import java.time.Duration
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Test

class ToolWeatherTest : ToolTestBase(Tools.WEATHER) {

    @Test
    fun showsTheWeatherFromTheDevicesSensors() {
        assumeTrue(Tools.isToolAvailable(context, Tools.WEATHER))

        clickOk()

        not(waitForTime = AutomationLibrary.GPS_WAIT_FOR_TIMEOUT) {
            hasText("Updating weather")
        }

        hasText(R.id.weather_title) {
            it == string(R.string.weather_no_change) ||
                    it == string(R.string.weather_overcast) ||
                    it == string(R.string.weather_clear)
        }

        waitFor(15000) {
            // The readings may still be loading while scrolling
            scrollToStart()
            scrollUntil { hasText(string(R.string.pressure)) }
        }
        hasText(Regex("\\d+(\\.\\d+)? in"))

        optional {
            hasText(string(R.string.temperature))
            hasText(string(R.string.historic_temperature_years, 30))
            hasText(Regex("\\d+(\\.\\d+)? °F"))
        }

        optional {
            hasText(string(R.string.temperature_high_low))
            hasText(Regex("\\d+(\\.\\d+)? °F / \\d+(\\.\\d+)? °F"))
        }

        scrollUntil { hasText(string(R.string.last_updated)) }
        scrollToStart()

        isVisible(R.id.chart)

        optional {
            click(string(R.string.temperature_high_low))
            hasText(string(R.string.next_24_hours))
            isVisible(R.id.chart)
            clickOk()
        }
    }

    @Test
    fun canUseWeatherMonitor() {
        assumeTrue(Tools.isToolAvailable(context, Tools.WEATHER))

        clickOk()

        hasText(R.id.play_bar_title, "Off - 30m")
        click(R.id.play_btn)

        if (AutomationLibrary.packageName == null) {
            hasNotification(
                WeatherMonitorService.WEATHER_NOTIFICATION_ID,
                title = string(R.string.weather)
            )
        }

        optional {
            hasText(string(R.string.battery_settings_limit_accuracy))
            doesNotHaveText(string(R.string.battery_settings_limit_accuracy))
        }

        hasText(R.id.play_bar_title, "On - 30m")

        click(R.id.play_btn)

        if (AutomationLibrary.packageName == null) {
            doesNotHaveNotification(WeatherMonitorService.WEATHER_NOTIFICATION_ID)
        }
    }

    @Test
    fun canUseWeatherMonitorFromTheQuickAction() {
        assumeTrue(Tools.isToolAvailable(context, Tools.WEATHER))

        clickOk()

        TestUtils.openQuickActions()
        clickQuickAction(Tools.QUICK_ACTION_WEATHER_MONITOR)

        if (AutomationLibrary.packageName == null) {
            hasNotification(
                WeatherMonitorService.WEATHER_NOTIFICATION_ID,
                title = string(R.string.weather)
            )
        }

        optional {
            hasText(string(R.string.battery_settings_limit_accuracy))
            doesNotHaveText(string(R.string.battery_settings_limit_accuracy))
        }

        clickQuickAction(Tools.QUICK_ACTION_WEATHER_MONITOR)

        if (AutomationLibrary.packageName == null) {
            doesNotHaveNotification(WeatherMonitorService.WEATHER_NOTIFICATION_ID)
        }

        TestUtils.closeQuickActions()
    }

    // Seeds known pressure histories and verifies the forecast shown to the user, covering the whole
    // pipeline from stored readings to the weather screen.

    @Test
    fun steadyPressureHasNoTendency() {
        openWithPressureTrend(1020f, 1020f, Duration.ofHours(6))

        hasTendency(0f)
        hasText("1020.0 hPa", exact = true)
        doesNotHaveText(string(R.string.weather_storm), waitForTime = 0)
    }

    @Test
    fun fallingPressureShowsTheThreeHourDrop() {
        openWithPressureTrend(1018f, 1012f, Duration.ofHours(6))

        // 6 hPa lost over 6 hours is a 3 hPa drop every 3 hours
        hasTendency(-3f)
        hasText("1012.0 hPa", exact = true)
        doesNotHaveText(string(R.string.weather_storm), waitForTime = 0)
    }

    @Test
    fun rapidlyFallingPressureShowsAStormAlert() {
        openWithPressureTrend(1020f, 1010f, Duration.ofHours(3))

        hasText(R.id.weather_title) {
            it.startsWith(string(R.string.precipitation_rain)) || it.startsWith(string(R.string.precipitation_snow))
        }
        hasText(R.id.weather_title, string(R.string.weather_within_hours, 3))
        hasTendency(-10f)
        hasText("1010.0 hPa", exact = true)

        hasText(string(R.string.alerts), exact = true)
        hasText(string(R.string.weather_storm))
        click(string(R.string.alerts), exact = true)
        hasText(string(R.string.weather_alert_storm_description))
        clickOk()
    }

    @Test
    fun risingPressureShowsTheThreeHourRise() {
        openWithPressureTrend(1008f, 1016f, Duration.ofHours(6))

        hasTendency(4f)
        hasText("1016.0 hPa", exact = true)
        doesNotHaveText(string(R.string.weather_storm), waitForTime = 0)
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
        doesNotHaveText(string(R.string.weather_storm), waitForTime = 0)
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

    // WHY: The forecast removes the daily pressure tide, which shifts the tendency by up to about 0.2 hPa at Tromsø depending on the time of day
    private fun hasTendency(hpa: Float) {
        val accepted = (-TENDENCY_TOLERANCE_HUNDREDTHS..TENDENCY_TOLERANCE_HUNDREDTHS).map { "%.2f hPa / 3h".format(hpa + it / 100f) }.distinct()
        hasText(Regex(accepted.joinToString("|") { Regex.escape(it) }), exact = true)
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
            TestData.addPressureTrend(start, end, duration, interval = interval, location = TROMSO)
        }
        optional { clickOk(waitForTime = 1000) }
    }

    companion object {
        // WHY: The daily pressure tide is strongest at the equator and much weaker near the poles, so the tendency barely depends on the time of day
        private val TROMSO = Coordinate(69.65, 18.96)
        private const val TENDENCY_TOLERANCE_HUNDREDTHS = 25
    }
}
