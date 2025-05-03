package com.kylecorry.trail_sense.tools.climate

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils.pickDate
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolClimateTest : ToolTestBase(Tools.CLIMATE) {

    @Test
    fun verifyBasicFunctionality() {
        // Verify today is selected by default
        hasText(R.id.display_date, string(R.string.today))
        input(R.id.utm, "42, -72")
        input(R.id.elevation_input, "1000")

        // Change the date
        click(R.id.date_btn)
        pickDate(2024, 8, 5)

        // Verify the temperature is displayed
        hasText(R.id.temperature_title, "81 °F / 59 °F", contains = true)

        // Scroll until precipitation section is visible, then verify it
        scrollUntil(R.id.climate_scroll) {
            hasText(R.id.precipitation_title, "Precipitation: 3.8 in monthly total")
        }

        // Continue scrolling to find climate zone section
        scrollUntil(R.id.climate_scroll) {
            hasText(R.id.climate_zone_title, string(R.string.climate_continental))
            hasText(
                R.id.climate_zone_description, listOf(
                    string(R.string.climate_continental_description),
                    string(R.string.climate_no_dry_season_description),
                    string(R.string.climate_warm_summer_description)
                ).joinToString(" ")
            )
        }
    }
}