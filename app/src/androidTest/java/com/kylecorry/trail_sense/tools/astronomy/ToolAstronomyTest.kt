package com.kylecorry.trail_sense.tools.astronomy

import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils.isDarkThemeOn
import com.kylecorry.trail_sense.test_utils.AutomationLibrary
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.longClick
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.handleExactAlarmsDialog
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ToolAstronomyTest : ToolTestBase(Tools.ASTRONOMY, location) {

    @Test
    fun showsTheSunAndMoonForToday() {
        // Verify the title
        hasText(R.id.astronomy_title) {
            val valid = listOf(
                string(R.string.until_sunset),
                string(R.string.until_sunrise)
            )
            valid.contains(it)
        }

        hasText(R.id.astronomy_title, Regex("([0-9]+h)? ?([0-9]+m)?"))

        // Verify that today is selected
        hasText(R.id.display_date, string(R.string.today))

        // Verify the list of astronomy events is displayed
        hasText(R.id.astronomy_detail_list) {
            it.startsWith(string(R.string.sun))
        }

        hasText(R.id.astronomy_detail_list) {
            it.startsWith(string(R.string.moon))
        }
    }

    @Test
    fun showsTheSunAndMoonTimesForADate() {
        goToDate(2024, 6, 21)
        hasText(R.id.date_btn, "June 21, 2024")

        // USNO Astronomical Applications API, 42.03 N 71.97 W, 2024-06-21 (EDT):
        // sunrise 5:12 AM, solar noon 12:50 PM, sunset 8:27 PM, moonset 4:15 AM, moonrise 8:47 PM
        hasEvent("2024-06-21T09:12:00Z", string(R.string.astronomy_rise))
        hasEvent("2024-06-21T16:50:00Z", string(R.string.noon))
        hasEvent("2024-06-22T00:27:00Z", string(R.string.astronomy_set))

        // USNO: Full Moon on 2024-06-21 with 100% of the moon illuminated
        scrollUntil {
            hasText(Regex("Full moon \\((99|100)%\\)"))
            hasEvent("2024-06-21T08:15:00Z", string(R.string.astronomy_set), toleranceMinutes = 3)
            hasEvent("2024-06-22T00:47:00Z", string(R.string.astronomy_rise), toleranceMinutes = 3)
        }
    }

    @Test
    fun canToggleTheSunsetAndSunriseAlerts() {
        TestUtils.openQuickActions()
        click({ quickAction(Tools.QUICK_ACTION_SUNSET_ALERT) })

        handleExactAlarmsDialog()

        click({ quickAction(Tools.QUICK_ACTION_SUNSET_ALERT) })

        click({ quickAction(Tools.QUICK_ACTION_SUNRISE_ALERT) })

        handleExactAlarmsDialog()

        click({ quickAction(Tools.QUICK_ACTION_SUNRISE_ALERT) })

        // TODO: Simulate time passing to verify the alerts are shown

        TestUtils.closeQuickActions()
    }

    @Test
    fun canViewTheSunAndMoonIn3D() {
        if (Tools.isToolAvailable(TestUtils.context, Tools.AUGMENTED_REALITY)) {
            // Wait for the toast do disappear
            not {
                hasText(
                    string(R.string.sunset_alerts_background_location_disclaimer)
                )
            }
            click(R.id.button_3d)

            // Verify the AR tool is open
            click("Cancel")
            clickOk()
            if (!Sensors.hasGyroscope(TestUtils.context)) {
                clickOk()
            }
            isVisible(R.id.ar_view)
        } else {
            isNotVisible(R.id.button_3d)
        }
    }

    @Test
    fun verifyNightMode() {
        TestUtils.openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_NIGHT_MODE))

        // TODO: Need to find a way to test this on staging builds
        if (AutomationLibrary.packageName == null) {
            waitFor {
                scenario.onActivity {
                    assertTrue(it.isDarkThemeOn())
                    // TODO: Verify the color filter is applied
                }
            }
        }

        TestUtils.openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_NIGHT_MODE))
    }

    @Test
    fun canMoveBetweenDates() {
        hasText(R.id.date_btn, string(R.string.today))

        click(R.id.next_date)
        hasText(R.id.date_btn, string(R.string.tomorrow))

        click(R.id.prev_date)
        click(R.id.prev_date)
        hasText(R.id.date_btn, string(R.string.yesterday))

        longClick(R.id.date_btn)
        hasText(R.id.date_btn, string(R.string.today))
    }

    @Test
    fun canFindTheNextFullMoonAndNewMoon() {
        goToDate(2025, 3, 1)

        findNext("Full Moon")
        // Full moon: March 14, 2025 06:55 UTC
        hasText(R.id.date_btn, Regex("March 1[345], 2025"), waitForTime = SEARCH_TIMEOUT)

        findNext("New Moon")
        // New moon: March 29, 2025 10:58 UTC
        hasText(R.id.date_btn, Regex("March (28|29|30), 2025"), waitForTime = SEARCH_TIMEOUT)
    }

    @Test
    fun timeSeekerFollowsTapsOnTheChart() {
        goToDate(2025, 6, 21)

        click(R.id.sunMoonChart, xPercent = 0.5f, yPercent = 0.5f)
        val middle = waitFor { view(R.id.seek_time).uiObject.text }

        click(R.id.sunMoonChart, xPercent = 0.9f, yPercent = 0.5f)
        waitFor {
            assertTrue(view(R.id.seek_time).uiObject.text != middle)
        }
    }

    private fun goToDate(year: Int, month: Int, day: Int) {
        click(R.id.date_btn)
        TestUtils.pickDate(year, month, day)
    }

    private fun findNext(event: String) {
        click(R.id.search_btn)
        click(event, exact = true)
        clickOk()
    }

    private fun hasEvent(utc: String, label: String, toleranceMinutes: Long = 2) {
        // The app's sun and moon models can differ from the published times by a few minutes
        val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
        val time = Instant.parse(utc)
        val allowed = (-toleranceMinutes..toleranceMinutes).joinToString("|") {
            Regex.escape(formatter.format(time.plusSeconds(it * 60).atZone(ZoneId.systemDefault())))
        }
        hasText(Regex("($allowed)\\s+${Regex.escape(label)}"))
    }

    companion object {
        private val location = Coordinate(42.03, -71.97)
        private const val SEARCH_TIMEOUT = 30000L
    }
}
