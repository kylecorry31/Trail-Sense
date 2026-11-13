package com.kylecorry.trail_sense.tools.turn_back

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.GPS_WAIT_FOR_TIMEOUT
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.optional
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils.handleExactAlarmsDialog
import com.kylecorry.trail_sense.test_utils.TestUtils.pickTime
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ToolTurnBackTest : ToolTestBase(Tools.TURN_BACK, Coordinate(42.03, -71.97)) {

    @Test
    fun canSetAndCancelAReturnTime() {
        hasText(R.id.instructions, string(R.string.time_not_set))

        // Enter a time
        click(R.id.edittext)
        val before = ZonedDateTime.now()
        pickTime(8, 0, false)
        val after = ZonedDateTime.now()

        handleExactAlarmsDialog()

        hasText(R.id.edittext, "8:00 PM")
        // Turning back after 40% of the time until the return time has passed
        hasText(
            R.id.instructions,
            Regex("Turn around by (${turnAroundTimes(before, after)}) \\(.*\\) to return at 8:00 PM")
        )
        click(R.id.cancel_button)
        hasText(R.id.instructions, string(R.string.time_not_set))
    }

    @Test
    fun canReturnBeforeDark() {
        click(R.id.sunset_button)

        // Loading dialog is possible
        optional {
            hasText(string(R.string.loading))
            not(GPS_WAIT_FOR_TIMEOUT) { hasText(string(R.string.loading)) }
        }

        handleExactAlarmsDialog()

        // The next sunset depends on today's date, but at 42 N it is always between 4 PM and 9 PM
        hasText(
            R.id.edittext,
            Regex("([4-8]):\\d+ PM.*"),
            waitForTime = GPS_WAIT_FOR_TIMEOUT
        )
    }

    private fun turnAroundTimes(before: ZonedDateTime, after: ZonedDateTime): String {
        val zone = ZoneId.systemDefault()
        fun turnAround(now: ZonedDateTime): Instant {
            var returnTime = now.with(LocalTime.of(20, 0))
            if (returnTime < now) {
                returnTime = returnTime.plusDays(1)
            }
            val remaining = Duration.between(now, returnTime).toMillis()
            return now.toInstant().plusMillis((remaining * 0.4).toLong())
        }

        // The time is shown to the minute, and the pickers add a little delay
        val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
        val start = turnAround(before).atZone(zone).minusMinutes(1)
        val end = turnAround(after).atZone(zone).plusMinutes(1)
        val times = mutableListOf<String>()
        var current = start
        while (current <= end) {
            times.add(Regex.escape(formatter.format(current)))
            current = current.plusMinutes(1)
        }
        return times.distinct().joinToString("|")
    }
}
