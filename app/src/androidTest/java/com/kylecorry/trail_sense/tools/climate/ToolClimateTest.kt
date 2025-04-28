package com.kylecorry.trail_sense.tools.climate

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
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
    }
}