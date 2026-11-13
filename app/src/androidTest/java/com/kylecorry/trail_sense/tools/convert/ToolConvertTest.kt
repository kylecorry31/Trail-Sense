package com.kylecorry.trail_sense.tools.convert

import androidx.test.uiautomator.Direction
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.doesNotHaveNotification
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasNotification
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.optional
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherMonitorService
import org.junit.Test

class ToolConvertTest : ToolTestBase(Tools.CONVERT) {
    @Test
    fun convertsCoordinates() {
        canConvertCoordinates()
    }

    @Test
    fun convertsMetersToFeetAndBack() {
        canConvertDistance()
    }

    @Test
    fun convertsCelsiusToFahrenheitAndBack() {
        canConvertTemperature()
    }

    @Test
    fun convertsLitersToGallonsAndBack() {
        canConvertVolume()
    }

    @Test
    fun convertsKilogramsToPoundsAndBack() {
        canConvertWeight()
    }

    @Test
    fun convertsMinutesToHoursAndBack() {
        canConvertTime()
    }

    @Test
    fun canConvertFromTheQuickAction() {
        verifyQuickAction()
    }

    private fun canConvertCoordinates() {
        input(R.id.utm, "42, -72")
        click(R.id.to_units)
        click(string(R.string.coordinate_format_usng))
        // UTM 19T 251535 E 4654131 N (transverse Mercator series, 4654130.9 rounded), 100 km square BG
        hasText(R.id.result, "19T BG 51535 54131")
    }

    private fun canConvertDistance() {
        selectCategory(string(R.string.distance))
        hasText(string(R.string.unit_meters))
        input(R.id.unit_edit, "1")

        click(R.id.from_units)
        click(string(R.string.unit_meters))

        click(R.id.to_units)
        click(string(R.string.unit_feet))

        // 1 m = 3.28084 ft
        hasText(R.id.result, "3.2808 ft")

        // Swap
        click(R.id.swap_btn)
        // 1 ft = 0.3048 m
        hasText(R.id.result, "0.3048 m")
    }

    private fun canConvertTemperature() {
        selectCategory(string(R.string.temperature))
        hasText(string(R.string.celsius))
        input(R.id.unit_edit, "0")
        click(R.id.from_units)
        click(string(R.string.celsius))

        click(R.id.to_units)
        click(string(R.string.fahrenheit))

        // 0 C * 9/5 + 32 = 32 F
        hasText(R.id.result, "32 °F")

        // Swap
        click(R.id.swap_btn)
        // (0 F - 32) * 5/9 = -17.7778 C
        hasText(R.id.result, "-17.7778 °C")
    }

    private fun canConvertVolume() {
        selectCategory(string(R.string.volume))
        hasText(string(R.string.liters))
        input(R.id.unit_edit, "1")
        click(R.id.from_units)
        click(string(R.string.liters))

        click(R.id.to_units)
        click(string(R.string.us_gallons))

        // 1 L = 0.264172 US gal
        hasText(R.id.result, "0.2642 gal")

        // Swap
        click(R.id.swap_btn)
        // 1 US gal = 3.785411 L
        hasText(R.id.result, "3.7854 l")
    }

    private fun canConvertWeight() {
        selectCategory(string(R.string.weight))
        hasText(string(R.string.kilograms))
        input(R.id.unit_edit, "1")
        click(R.id.from_units)
        click(string(R.string.kilograms))

        click(R.id.to_units)
        click(string(R.string.pounds))

        // 1 kg = 2.204623 lb
        hasText(R.id.result, "2.2046 lb")

        // Swap
        click(R.id.swap_btn)
        // 1 lb = 0.453592 kg
        hasText(R.id.result, "0.4536 kg")
    }

    private fun canConvertTime() {
        selectCategory(string(R.string.time))
        hasText(string(R.string.minutes))
        input(R.id.unit_edit, "60")
        click(R.id.from_units)
        click(string(R.string.minutes))

        click(R.id.to_units)
        click(string(R.string.hours))

        // 60 min / 60 = 1 h
        hasText(R.id.result, "1 h")

        // Swap
        click(R.id.swap_btn)
        // 60 h * 60 = 3600 min
        hasText(R.id.result, "3600 m")
    }

    // The categories scroll horizontally, so the later ones start off screen
    private fun selectCategory(name: String) {
        scrollUntil(direction = Direction.RIGHT) {
            click(name)
        }
    }

    private fun verifyQuickAction() {
        TestUtils.openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_CONVERT))

        canConvertCoordinates()
        canConvertDistance()
        canConvertTemperature()

        back()

        TestUtils.closeQuickActions()
    }

}
