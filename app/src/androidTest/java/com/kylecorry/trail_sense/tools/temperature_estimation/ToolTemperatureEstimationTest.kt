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
    fun adjustsTheTemperatureForElevation() {
        input(R.id.temp_est_base_temperature, "15")
        hasText(R.id.temperature_title, "15 °F")

        input(R.id.temp_est_base_elevation, "100")
        input(R.id.temp_est_dest_elevation, "1000")
        // 900 ft = 274.3 m at 6.5 C/km = 1.78 C = 3.2 F lower: 15 - 3.2 = 11.8 F
        hasText(R.id.temperature_title, "12 °F")
    }

    @Test
    fun adjustsTheTemperatureForHumidity() {
        input(R.id.temp_est_base_temperature, "90")
        input(R.id.temp_est_humidity, "70")
        // NWS heat index (Rothfusz regression) at 90 F and 70% humidity is 105.9 F
        hasText(R.id.temperature_title, "106 °F")
    }

    @Test
    fun adjustsTheTemperatureForWind() {
        input(R.id.temp_est_base_temperature, "40")
        input(R.id.temp_est_wind_speed, "10")
        // NWS wind chill at 40 F and 10 mph is 33.6 F
        hasText(R.id.temperature_title, "34 °F")
    }

    @Test
    fun canAutofillTheBaseValues() {
        input(R.id.temp_est_base_elevation, "")
        input(R.id.temp_est_base_temperature, "")

        // The values come from the emulator's sensors, so only check that they are numbers
        click(R.id.temp_est_autofill)
        hasText(R.id.temp_est_base_elevation, waitForTime = GPS_WAIT_FOR_TIMEOUT) {
            it.split(",").first().toFloatCompat() != null
        }
        hasText(R.id.temp_est_base_temperature) {
            it.split(",").first().toFloatCompat() != null
        }
    }
}
