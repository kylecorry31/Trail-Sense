package com.kylecorry.trail_sense.tools.tides

import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.GPS_WAIT_FOR_TIMEOUT
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollToEnd
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestData
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.clickListItemMenu
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Assume.assumeFalse
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ToolTidesTest : ToolTestBase(Tools.TIDES, boston) {

    @Test
    fun canCreateAndViewATideTable() {
        // Wait for the disclaimer
        clickOk()

        // Wait for the nearby tides to load
        hasText(string(R.string.navigation_nearby_category), waitForTime = GPS_WAIT_FOR_TIMEOUT)
        click(toolbarButton(R.id.tide_title, Side.Right))

        // Tide list
        hasText(R.id.tide_list_title, string(R.string.tides))
        hasText(string(R.string.navigation_nearby_category))
        hasText(string(R.string.tide_model_auto))

        canCreateTide()
        canViewTide()
        canOpenTideList()
    }

    @Test
    fun canEditATideTable() {
        assumeFalse(isStagingBuild)
        relaunchTool {
            TestData.addTideTable(
                "Tide 1",
                Coordinate(42.0, -72.0),
                listOf(Tide(ZonedDateTime.now(), true, 0.3048f))
            )
        }
        clickOk()
        click(toolbarButton(R.id.tide_title, Side.Right))

        clickListItemMenu(string(R.string.edit))

        hasText(R.id.create_tide_title, string(R.string.tide_table))

        // Verify the fields are set
        hasText(R.id.tide_name) { it.startsWith("Tide 1") }

        // Select lunitidal interval
        click(R.id.estimate_algorithm_spinner)
        click(string(R.string.lunitidal_interval_auto))
        clickOk()

        // Verify the fields are set
        hasText(R.id.estimate_algorithm_spinner, string(R.string.lunitidal_interval_auto))

        // TODO: Manual lunitidal interval

        // Save
        click(toolbarButton(R.id.create_tide_title, Side.Right))

        hasText(R.id.tide_list_title, string(R.string.tides))
        hasText("Tide 1")

        // Open the tide
        click("Tide 1")

        // Verify it is now using the lunitidal interval (just check times are shown)
        hasText(R.id.tide_title, string(R.string.high_tide))
    }

    @Test
    fun showsTheTidesOfASeededTideTable() {
        assumeFalse(isStagingBuild)
        // NOAA CO-OPS predictions, Boston (8443970), MLLW, metric, hilo, GMT
        val published = listOf(
            Triple("2024-06-20T02:14:00Z", true, 3.042f),
            Triple("2024-06-20T08:43:00Z", false, 0.206f),
            Triple("2024-06-20T14:54:00Z", true, 2.622f),
            Triple("2024-06-20T20:46:00Z", false, 0.493f),
            Triple("2024-06-21T02:57:00Z", true, 3.13f),
            Triple("2024-06-21T09:27:00Z", false, 0.103f),
            Triple("2024-06-21T15:39:00Z", true, 2.666f),
            Triple("2024-06-21T21:30:00Z", false, 0.435f),
            Triple("2024-06-22T03:40:00Z", true, 3.21f),
            Triple("2024-06-22T10:11:00Z", false, 0.013f),
            Triple("2024-06-22T16:24:00Z", true, 2.715f),
            Triple("2024-06-22T22:15:00Z", false, 0.376f)
        )
        relaunchTool {
            TestData.addTideTable(
                "Boston",
                boston,
                published.map { Tide(Instant.parse(it.first).atZone(ZoneOffset.UTC), it.second, it.third) }
            )
        }
        clickOk()

        click(toolbarButton(R.id.tide_title, Side.Right))
        click("Boston")
        hasText(R.id.tide_title, "Boston")
        click(R.id.date_btn)
        TestUtils.pickDate(2024, 6, 21)

        // The displayed day is in the device time zone
        val day = LocalDate.of(2024, 6, 21)
        val zone = ZoneId.systemDefault()
        val expected = published.filter { Instant.parse(it.first).atZone(zone).toLocalDate() == day }
        // The list is taller than the screen, so scroll through it in order
        expected.forEach { (time, _, heightMeters) ->
            scrollUntil {
                hasText(withinTwoMinutes(Instant.parse(time), zone), exact = true)
                hasText(
                    String.format(Locale.US, "%.2f ft", heightMeters * METERS_TO_FEET),
                    exact = true
                )
            }
        }
    }

    private fun withinTwoMinutes(time: Instant, zone: ZoneId): Regex {
        // The app finds the extremes of the water level numerically
        val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
        val allowed = (-2L..2L).joinToString("|") {
            Regex.escape(formatter.format(time.plusSeconds(it * 60).atZone(zone)))
        }
        return Regex(allowed)
    }

    private fun canCreateTide() {
        // Click the add button
        click(R.id.add_btn)

        // Enter the tide details
        hasText(R.id.create_tide_title, string(R.string.tide_table))
        input(R.id.tide_name, "Tide 1")
        isChecked(R.id.tide_frequency_semidiurnal)
        input(R.id.utm, "42, -72")

        click(R.id.estimate_algorithm_spinner)
        isChecked(string(R.string.tide_clock))
        hasText(string(R.string.lunitidal_interval_auto))
        clickOk()

        scrollToEnd(R.id.scroll_view)

        hasText(R.id.tide_type, string(R.string.high_tide_letter))
        click(R.id.tide_type)
        hasText(R.id.tide_type, string(R.string.low_tide_letter))
        click(R.id.tide_type)
        hasText(R.id.tide_type, string(R.string.high_tide_letter))

        click(R.id.tide_time, index = 1)
        clickOk()
        clickOk()

        // TODO: Verify the time is set
        hasText(R.id.tide_time, index = 1) { it.isNotBlank() }

        click(R.id.tide_height, index = 1)
        input(string(R.string.distance), "1.0")
        clickOk()

        hasText(R.id.tide_height, "1.00 ft", index = 1)

        click(R.id.add_tide_entry)

        hasText(R.id.tide_type, string(R.string.high_tide_letter), index = 1)
        click(R.id.delete, index = 1)

        isNotVisible(R.id.tide_type, index = 1)

        click(toolbarButton(R.id.create_tide_title, Side.Right))

        hasText(R.id.tide_list_title, string(R.string.tides))
        hasText("Tide 1")
        hasText("1 tide", exact = true)
    }

    private fun canViewTide() {
        click("Tide 1")
        hasText(R.id.tide_title, string(R.string.high_tide))
        hasText(R.id.tide_title, "Tide 1")

        // Verify that today is selected
        hasText(R.id.tide_list_date, string(R.string.today))

        // Verify at least one high and low tide is shown
        var isHighFirst = false
        hasText(com.kylecorry.andromeda.views.R.id.title) {
            isHighFirst = it == string(R.string.high_tide)
            it == string(R.string.high_tide) || it == string(R.string.low_tide)
        }

        hasText(
            com.kylecorry.andromeda.views.R.id.title, string(
                if (isHighFirst) {
                    R.string.low_tide
                } else {
                    R.string.high_tide
                }
            ), index = 1
        )

        // TODO: Verify the times are correct
        // TODO: Verify the chart
    }

    private fun canOpenTideList() {
        click(toolbarButton(R.id.tide_title, Side.Right))

        hasText(R.id.tide_list_title, string(R.string.tides))
        hasText("Tide 1")
    }

    companion object {
        private val boston = Coordinate(42.3539, -71.0503)
        private const val METERS_TO_FEET = 3.28084f
    }
}
