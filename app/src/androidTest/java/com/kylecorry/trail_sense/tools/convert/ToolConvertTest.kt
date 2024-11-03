package com.kylecorry.trail_sense.tools.convert

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ToolConvertTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    val grantPermissionRule = TestUtils.mainPermissionsGranted()

    @get:Rule
    val instantExec = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        hiltRule.inject()
        TestUtils.setWaitForIdleTimeout()
        TestUtils.setupApplication()
        TestUtils.startWithTool(Tools.CONVERT)
    }

    @Test
    fun verifyBasicFunctionality() {
        canConvertCoordinates()
        canConvertDistance()
        canConvertTemperature()
        canConvertVolume()
        canConvertWeight()
        canConvertTime()
    }

    private fun canConvertCoordinates() {
        input(R.id.utm, "42, -72")
        click(R.id.to_units)
        click(string(R.string.coordinate_format_usng))
        hasText(R.id.result, "19T BG 51535 54131")
    }

    private fun canConvertDistance() {
        click(string(R.string.distance))
        hasText(string(R.string.unit_meters))
        input(R.id.unit_edit, "1")

        click(R.id.from_units)
        click(string(R.string.unit_meters))

        click(R.id.to_units)
        click(string(R.string.unit_feet))

        hasText(R.id.result, "3.2808 ft")

        // Swap
        click(R.id.swap_btn)
        hasText(R.id.result, "0.3048 m")
    }

    private fun canConvertTemperature() {
        click(string(R.string.temperature))
        hasText(string(R.string.celsius))
        input(R.id.unit_edit, "0")
        click(R.id.from_units)
        click(string(R.string.celsius))

        click(R.id.to_units)
        click(string(R.string.fahrenheit))

        hasText(R.id.result, "32 °F")

        // Swap
        click(R.id.swap_btn)
        hasText(R.id.result, "-17.7778 °C")
    }

    private fun canConvertVolume() {
        click(string(R.string.volume))
        hasText(string(R.string.liters))
        input(R.id.unit_edit, "1")
        click(R.id.from_units)
        click(string(R.string.liters))

        click(R.id.to_units)
        click(string(R.string.us_gallons))

        hasText(R.id.result, "0.2642 gal")

        // Swap
        click(R.id.swap_btn)
        hasText(R.id.result, "3.7854 l")
    }

    private fun canConvertWeight() {
        click(string(R.string.weight))
        hasText(string(R.string.kilograms))
        input(R.id.unit_edit, "1")
        click(R.id.from_units)
        click(string(R.string.kilograms))

        click(R.id.to_units)
        click(string(R.string.pounds))

        hasText(R.id.result, "2.2046 lb")

        // Swap
        click(R.id.swap_btn)
        hasText(R.id.result, "0.4536 kg")
    }

    private fun canConvertTime() {
        click(string(R.string.time))
        hasText(string(R.string.minutes))
        input(R.id.unit_edit, "60")
        click(R.id.from_units)
        click(string(R.string.minutes))

        click(R.id.to_units)
        click(string(R.string.hours))

        hasText(R.id.result, "1 h")

        // Swap
        click(R.id.swap_btn)
        hasText(R.id.result, "3600 m")
    }

}