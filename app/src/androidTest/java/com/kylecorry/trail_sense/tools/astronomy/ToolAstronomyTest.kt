package com.kylecorry.trail_sense.tools.astronomy

import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils.isDarkThemeOn
import com.kylecorry.trail_sense.test_utils.AutomationLibrary
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.delay
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.longClick
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollToEnd
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
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

class ToolAstronomyTest : ToolTestBase(Tools.ASTRONOMY, location) {

    @Test
    fun verifyBasicFunctionality() {
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

        verifyQuickActions()

        // Verify the View in 3D button is visible and works
        if (Tools.isToolAvailable(TestUtils.context, Tools.AUGMENTED_REALITY)) {
            // Wait for the toast do disappear
            not {
                hasText(
                    string(R.string.sunset_alerts_background_location_disclaimer)
                )
            }
            click(R.id.button_3d)

            // Verify the AR tool is open
            delay(500)
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

    private fun verifyQuickActions() {
        // Verify the sunset alert quick action
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

    // Checks the astronomy screen against known events at a fixed location and fixed dates. The
    // expected values were verified against published sun, moon, and eclipse data for the area
    // (the location is about 1 degree west of Boston, MA).

    @Test
    fun showsSunAndMoonTimesForADate() {
        goToDate(2025, 6, 21)

        hasText(R.id.date_btn, "June 21, 2025")

        // Summer solstice: 15h 18m of daylight
        hasText(Regex("Sun\\s+•\\s+15h 18m daylight"))
        hasText("5:10 AM\nRise")
        hasText("12:49 PM\nNoon")
        hasText("8:29 PM\nSet")

        hasText(Regex("Moon\\s+•\\s+Waning crescent \\(19%\\)"))
        hasText("1:51 AM\nRise")
        hasText("9:00 AM\nNoon")
        hasText("4:25 PM\nSet")
    }

    @Test
    fun showsDifferentValuesInWinter() {
        goToDate(2025, 12, 21)

        // Winter solstice: 9h 3m of daylight
        hasText(Regex("Sun\\s+•\\s+9h \\d+m daylight"))
        hasText(Regex("7:\\d\\d AM\nRise"))
        hasText(Regex("4:\\d\\d PM\nSet"))
    }

    @Test
    fun sunDetailsShowTwilightTimesAndDaylight() {
        goToDate(2025, 6, 21)

        click("Sun")

        hasText(string(R.string.sun_actual))
        hasText("5:10 AM\nRise")
        hasText(string(R.string.sun_civil))
        hasText("4:37 AM\nRise")
        hasText("9:02 PM\nSet")
        hasText(string(R.string.sun_nautical))
        hasText("3:53 AM\nRise")
        hasText("9:46 PM\nSet")
        hasText(string(R.string.sun_astronomical))
        hasText("2:59 AM\nRise")
        hasText("10:40 PM\nSet")

        // 90 - latitude + the solar declination at the solstice
        hasText(string(R.string.astronomy_altitude_peak))
        hasText("71°")
        hasText(string(R.string.daylight))
        hasText("15h 18m")
        hasText(string(R.string.night))
        hasText("8h 41m")
        clickOk()
    }

    @Test
    fun moonDetailsShowPhaseAndIllumination() {
        goToDate(2025, 6, 21)

        click("Moon")

        hasText(string(R.string.moon_phase))
        hasText("Waning crescent")
        hasText(string(R.string.illumination))
        hasText("19%")
        hasText(string(R.string.astronomy_altitude_peak))
        hasText("65°")
        hasText(string(R.string.supermoon))
        hasText(string(R.string.no), exact = true)
        clickOk()
    }

    @Test
    fun showsMeteorShowerOnItsPeak() {
        goToDate(2025, 8, 12)

        scrollUntil(R.id.astronomy_detail_list) {
            hasText(Regex("Meteor shower\\s+•\\s+Max 100 meteors / h"))
        }
        scrollToEnd(R.id.astronomy_detail_list)
        // Perseids
        hasTime("4:01 AM", "Peak")
    }

    @Test
    fun showsTotalLunarEclipse() {
        goToDate(2025, 3, 14)

        // The full moon is the same day
        hasText(Regex("Moon\\s+•\\s+Full moon \\(100%\\)"))

        scrollUntil(R.id.astronomy_detail_list) {
            hasText(Regex("Lunar eclipse\\s+•\\s+Total"))
        }
        scrollToEnd(R.id.astronomy_detail_list)
        hasTime("1:10 AM", "Start")
        hasTime("2:58 AM", "Peak")
        hasTime("4:47 AM", "End")
    }

    @Test
    fun showsPartialSolarEclipse() {
        goToDate(2024, 4, 8)

        // The eclipse happens at the new moon
        hasText(Regex("Moon\\s+•\\s+New moon \\(0%\\)"))

        scrollUntil(R.id.astronomy_detail_list) {
            hasText(Regex("Solar eclipse\\s+•\\s+Partial \\(94%\\)"))
        }
        scrollToEnd(R.id.astronomy_detail_list)
        hasTime("2:15 PM", "Start")
        hasTime("3:29 PM", "Peak")
        hasTime("4:38 PM", "End")
    }

    @Test
    fun doesNotShowEventsOnOrdinaryDays() {
        goToDate(2025, 6, 21)

        scrollToEnd(R.id.astronomy_detail_list)
        not { hasText("Meteor shower") }
        not { hasText("Lunar eclipse") }
        not { hasText("Solar eclipse") }
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
    fun canFindTheNextLunarEclipse() {
        goToDate(2025, 3, 1)

        findNext("Lunar Eclipse")

        hasText(R.id.date_btn, "March 14, 2025", waitForTime = SEARCH_TIMEOUT)
    }

    @Test
    fun timeSeekerShowsTheSunAndMoonPositionAtATime() {
        goToDate(2025, 6, 21)

        // Tapping the chart shows the positions of the sun and moon at that time
        click(R.id.sunMoonChart)
        isVisible(R.id.seek_time)
        hasText(R.id.sun_position_text, string(R.string.sun))
        hasText(R.id.moon_position_text, string(R.string.moon))

        // Altitude from the hour angle: sin(alt) = sin(lat)sin(dec) + cos(lat)cos(dec)cos(H)
        // using the 12:49 PM solar noon and the declination at the June solstice (23.44 degrees)
        val time = LocalTime.parse(
            view(R.id.seek_time).uiObject.text,
            DateTimeFormatter.ofPattern("h:mm a", Locale.US)
        )
        val hoursFromNoon = (time.toSecondOfDay() - LocalTime.of(12, 49).toSecondOfDay()) / 3600.0
        val latitude = Math.toRadians(location.latitude)
        val declination = Math.toRadians(23.44)
        val expectedAltitude = Math.toDegrees(
            asin(
                sin(latitude) * sin(declination) +
                        cos(latitude) * cos(declination) * cos(Math.toRadians(15 * hoursFromNoon))
            )
        )
        waitFor {
            val altitude = Regex("Altitude: (-?\\d+)°")
                .find(view(R.id.sun_position_text).uiObject.text)!!
                .groupValues[1].toInt()
            // Displayed to the nearest degree and the seconds of the time are not displayed
            assertTrue(
                "Expected $expectedAltitude but was $altitude",
                abs(altitude - expectedAltitude) < 1.5
            )
        }

        // The list of details is replaced by the seeker until it is closed
        isNotVisible(R.id.astronomy_detail_list)
        click(R.id.close_seek)
        isVisible(R.id.astronomy_detail_list)
        hasText(Regex("Sun\\s+•.*"))
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

    private fun hasTime(time: String, label: String) {
        hasText(Regex("\\s*" + Regex.escape(time) + "\\s+" + label))
    }

    private fun findNext(event: String) {
        click(R.id.search_btn)
        click(event, exact = true)
        clickOk()
    }

    companion object {
        private val location = Coordinate(42.03, -71.97)
        private const val SEARCH_TIMEOUT = 30000L
    }
}
