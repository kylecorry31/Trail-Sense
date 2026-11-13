package com.kylecorry.trail_sense.tools.clock

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.GPS_WAIT_FOR_TIMEOUT
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.any
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils.handleExactAlarmsDialog
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isTrue
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ToolClockTest : ToolTestBase(Tools.CLOCK) {

    @Test
    fun showsTheCurrentDateAndTime() {
        // The clock follows the device (or GPS) time, so compare it to the device clock
        isTrue {
            val shown = LocalTime.parse(
                view(R.id.clock_title, childId = R.id.toolbar_title)
                    .uiObject.text,
                DateTimeFormatter.ofPattern("h:mm:ss a", Locale.US)
            )
            Duration.between(shown, LocalTime.now()).abs() < Duration.ofSeconds(30)
        }

        val today = ZonedDateTime.now()
        hasText(
            R.id.clock_title,
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.US).format(today)
        )
    }

    @Test
    fun showsTheClockOffsetFromTheGps() {
        // Wait for the GPS to be found
        hasText(R.id.pip_button, string(R.string.pip_button), waitForTime = GPS_WAIT_FOR_TIMEOUT)
        // The offset depends on the emulator's GPS time
        any(
            {
                hasText(
                    R.id.clock_offset,
                    string(R.string.clock_device_time_accurate),
                    waitForTime = 0
                )
            },
            {
                hasText(
                    R.id.clock_offset,
                    Regex("Device clock is \\d+ seconds? slow"),
                    waitForTime = 0
                )
            },
            {
                hasText(
                    R.id.clock_offset,
                    Regex("Device clock is \\d+ seconds? fast"),
                    waitForTime = 0
                )
            }
        )
    }

    @Test
    fun canUpdateTheTimeAndOpenTheTimeSettings() {
        hasText(R.id.pip_button, string(R.string.pip_button), waitForTime = GPS_WAIT_FOR_TIMEOUT)

        // Update time from GPS
        click(toolbarButton(R.id.clock_title, Side.Right))

        // Click on the PIP button
        click(R.id.pip_button, waitForTime = GPS_WAIT_FOR_TIMEOUT)

        handleExactAlarmsDialog()

        clickOk()

        // Verify the system time settings are opened
        // Check if the settings app is opened
        hasText("time")
    }
}
