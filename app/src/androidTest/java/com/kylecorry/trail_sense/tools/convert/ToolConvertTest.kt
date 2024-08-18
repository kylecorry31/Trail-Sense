package com.kylecorry.trail_sense.tools.convert

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.views.click
import com.kylecorry.trail_sense.test_utils.views.hasText
import com.kylecorry.trail_sense.test_utils.views.input
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.test_utils.views.viewWithText
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
        TestUtils.setWaitForIdleTimeout(100)
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
        waitFor {
            view(R.id.utm).input("42, -72")
        }
        view(R.id.to_units).click()
        waitFor {
            viewWithText(R.string.coordinate_format_usng).click()
        }
        waitFor {
            view(R.id.result).hasText("19T BG 51535 54131")
        }
    }

    private fun canConvertDistance() {
        viewWithText(R.string.distance).click()
        waitFor {
            viewWithText(R.string.unit_meters)
            view(R.id.unit_edit).input("1")
        }
        view(R.id.from_units).click()
        waitFor {
            viewWithText(R.string.unit_meters).click()
        }

        waitFor {
            view(R.id.to_units).click()
        }
        waitFor {
            viewWithText(R.string.unit_feet).click()
        }

        waitFor {
            view(R.id.result).hasText("3.2808 ft")
        }

        // Swap
        view(R.id.swap_btn).click()
        waitFor {
            view(R.id.result).hasText("0.3048 m")
        }
    }

    private fun canConvertTemperature() {
        viewWithText(R.string.temperature).click()
        waitFor {
            viewWithText(R.string.celsius)
            view(R.id.unit_edit).input("0")
        }
        view(R.id.from_units).click()
        waitFor {
            viewWithText(R.string.celsius).click()
        }

        waitFor {
            view(R.id.to_units).click()
        }
        waitFor {
            viewWithText(R.string.fahrenheit).click()
        }

        waitFor {
            view(R.id.result).hasText("32 °F")
        }

        // Swap
        view(R.id.swap_btn).click()
        waitFor {
            view(R.id.result).hasText("-17.7778 °C")
        }
    }

    private fun canConvertVolume() {
        viewWithText(R.string.volume).click()
        waitFor {
            viewWithText(R.string.liters)
            view(R.id.unit_edit).input("1")
        }
        view(R.id.from_units).click()
        waitFor {
            viewWithText(R.string.liters).click()
        }

        waitFor {
            view(R.id.to_units).click()
        }
        waitFor {
            viewWithText(R.string.us_gallons).click()
        }

        waitFor {
            view(R.id.result).hasText("0.2642 gal")
        }

        // Swap
        view(R.id.swap_btn).click()
        waitFor {
            view(R.id.result).hasText("3.7854 l")
        }
    }

    private fun canConvertWeight() {
        viewWithText(R.string.weight).click()
        waitFor {
            viewWithText(R.string.kilograms)
            view(R.id.unit_edit).input("1")
        }
        view(R.id.from_units).click()
        waitFor {
            viewWithText(R.string.kilograms).click()
        }

        waitFor {
            view(R.id.to_units).click()
        }
        waitFor {
            viewWithText(R.string.pounds).click()
        }

        waitFor {
            view(R.id.result).hasText("2.2046 lb")
        }

        // Swap
        view(R.id.swap_btn).click()
        waitFor {
            view(R.id.result).hasText("0.4536 kg")
        }
    }

    private fun canConvertTime() {
        viewWithText(R.string.time).click()
        waitFor {
            viewWithText(R.string.minutes)
            view(R.id.unit_edit).input("60")
        }
        view(R.id.from_units).click()
        waitFor {
            viewWithText(R.string.minutes).click()
        }

        waitFor {
            view(R.id.to_units).click()
        }
        waitFor {
            viewWithText(R.string.hours).click()
        }

        waitFor {
            view(R.id.result).hasText("1 h")
        }

        // Swap
        view(R.id.swap_btn).click()
        waitFor {
            view(R.id.result).hasText("3600 m")
        }
    }

}