package com.kylecorry.trail_sense.tools.temperature_estimation

import com.kylecorry.luna.text.toFloatCompat
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.GPS_WAIT_FOR_TIMEOUT
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolTemperatureEstimationTest : ToolTestBase(Tools.TEMPERATURE_ESTIMATION) {

    @Test
    fun verifyBasicFunctionality() {
        input(R.id.temp_est_base_temperature, "15")
        hasText(R.id.temperature_title, "15 °F")

        // Elevation adjustment
        input(R.id.temp_est_base_elevation, "100")
        input(R.id.temp_est_dest_elevation, "1000")
        hasText(R.id.temperature_title, "12 °F")

        // Humidity adjustment
        input(R.id.temp_est_dest_elevation, "")
        input(R.id.temp_est_base_temperature, "90")
        input(R.id.temp_est_humidity, "70")
        hasText(R.id.temperature_title, "106 °F")

        // Wind adjustment
        input(R.id.temp_est_humidity, "")
        input(R.id.temp_est_base_temperature, "40")
        input(R.id.temp_est_wind_speed, "10")
        hasText(R.id.temperature_title, "34 °F")

        // Clear all views
        input(R.id.temp_est_base_elevation, "")
        input(R.id.temp_est_base_temperature, "")
        input(R.id.temp_est_humidity, "")
        input(R.id.temp_est_wind_speed, "")

        // Autofill
        click(R.id.temp_est_autofill)
        hasText(R.id.temp_est_base_elevation, waitForTime = GPS_WAIT_FOR_TIMEOUT) {
            it.split(",").first().toFloatCompat() != null
        }
        hasText(R.id.temp_est_base_temperature) {
            it.split(",").first().toFloatCompat() != null
        }
    }
}
