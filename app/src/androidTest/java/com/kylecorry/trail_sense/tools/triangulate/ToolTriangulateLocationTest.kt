package com.kylecorry.trail_sense.tools.triangulate

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.backUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.optional
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollToStart
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.sol.units.Coordinate
import org.junit.Test

class ToolTriangulateLocationTest : ToolTestBase(Tools.TRIANGULATE_LOCATION) {

    @Test
    fun verifyBasicFunctionality() {
        verifyValidTriangulation()
        verifyTriangulationActions()
        verifyMyLocationTriangulation()
        verifyInvalidTriangulation()
        verifyReset()
    }

    private fun verifyValidTriangulation() {
        enterLocation("41.99, -72", "0", trueNorth = true)
        switchLocation(1, 2)
        enterLocation("42, -72.01", "90", trueNorth = true)

        scrollToStart()
        hasText(R.id.triangulate_title, "42.000000°,  -72.000000°")
    }

    private fun verifyTriangulationActions() {
        click(string(R.string.share_ellipsis))
        hasText(string(android.R.string.copy))
        hasText(string(R.string.qr_code))
        hasText(string(R.string.maps))
        hasText(string(R.string.share_action_send))
        back(false)

        click(string(R.string.beacon), exact = true)
        hasText(R.id.create_beacon_title, string(R.string.create_beacon))
        hasText(R.id.beacon_location, "42.000000°,  -72.000000°")
        back()
        click(string(R.string.dialog_leave))
        backUntil {
            hasText(R.id.triangulate_title, "42.000000°,  -72.000000°")
        }

        click(string(R.string.navigate))
        optional {
            clickOk()
        }
        isVisible(R.id.navigation_title)
        hasText(string(R.string.location))
        backUntil {
            hasText(R.id.triangulate_title, "42.000000°,  -72.000000°")
        }
    }

    private fun verifyMyLocationTriangulation() {
        click(string(R.string.my_location))
        enterLocation("40.5, 9.5", "295")
        switchLocation(2, 1)
        enterLocation("40, 10", "220")

        scrollToStart()
        val expected = Coordinate(40.229722, 10.252778)
        hasText(R.id.triangulate_title, message = "Expected location within 30 meters") {
            val actual = Coordinate.parse(it)
            actual != null && actual.distanceTo(expected) < 30
        }
    }

    private fun verifyInvalidTriangulation() {
        scrollUntil {
            input(R.id.utm, "40.5, 9.5")
        }

        scrollToStart()
        hasText(R.id.triangulate_title, string(R.string.could_not_triangulate))
    }

    private fun verifyReset() {
        scrollUntil {
            click(string(R.string.reset), exact = true)
        }
        isNotVisible(R.id.actions)
        scrollToStart()
        verifyLocationIsReset()
        switchLocation(1, 2)
        verifyLocationIsReset()
    }

    private fun verifyLocationIsReset() {
        scrollUntil {
            hasText(R.id.bearing, string(R.string.direction_not_set))
        }
    }

    private fun enterLocation(coordinate: String, bearing: String, trueNorth: Boolean = false) {
        scrollUntil {
            input(R.id.utm, coordinate)
        }
        scrollUntil {
            click(string(R.string.enter_manually))
        }
        input(R.id.bearing, bearing)
        if (trueNorth) {
            click(string(R.string.true_north))
        }
        clickOk()
    }

    private fun switchLocation(currentLocationNumber: Int, nextLocationNumber: Int) {
        scrollToStart()
        scrollUntil {
            click(string(R.string.location_number, currentLocationNumber))
        }
        scrollToStart()
        scrollUntil {
            click(string(R.string.location_number, nextLocationNumber))
        }
    }
}
