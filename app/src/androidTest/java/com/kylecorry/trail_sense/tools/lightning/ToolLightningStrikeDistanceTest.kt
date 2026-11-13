package com.kylecorry.trail_sense.tools.lightning

import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestData
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Assume.assumeFalse
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ToolLightningStrikeDistanceTest : ToolTestBase(Tools.LIGHTNING_STRIKE_DISTANCE) {

    @Test
    fun recordsTheDistanceOfAStrike() {
        // Verify it is ready to record lightning
        hasText(R.id.start_btn, string(R.string.lightning))

        // Click the start button
        click(R.id.start_btn)

        // Verify it is recording
        hasText(R.id.lightning_title, Regex("([0-9.]+) (ft|mi)"))
        hasText(R.id.start_btn, string(R.string.thunder))

        // Click the start button again
        click(R.id.start_btn)

        // Verify it recorded the distance
        hasText(R.id.lightning_title, Regex("([0-9.]+) (ft|mi)"))
        hasText(R.id.start_btn, string(R.string.lightning))
        hasText(R.id.lightning_title, string(R.string.lightning_dangerously_close))
    }

    @Test
    fun showsThePreviousStrikeAfterRecordingAnother() {
        click(R.id.start_btn)
        click(R.id.start_btn)

        var previousDistance: String? = null
        hasText(R.id.lightning_title) {
            val regex = Regex("([0-9.]+) (ft|mi)")
            if (regex.matches(it)) {
                previousDistance = it
                true
            } else {
                false
            }
        }

        // Run it again and verify the last distance shows at the bottom of the screen
        click(R.id.start_btn)
        click(R.id.start_btn)

        hasText(
            R.id.previous_strike,
            Regex("Last strike at [0-9]+:[0-9]+ [AP]M - $previousDistance")
        )
    }

    @Test
    fun showsTheLastSavedStrike() {
        assumeFalse(isStagingBuild)
        relaunchTool {
            val time = LocalDate.of(2026, 1, 1).atTime(14, 30).atZone(ZoneId.systemDefault())
            TestData.addLightningStrike(Distance.meters(1715f), time.toInstant())
        }

        // 5 s * 343 m/s = 1715 m = 5627 ft = 1.07 mi, the time is shown in the 12 hour format
        hasText(
            R.id.previous_strike,
            string(
                R.string.dash_separated_pair,
                string(R.string.last_lightning_strike_at, "2:30 PM"),
                "1.07 mi"
            ),
            exact = true
        )
    }
}
