package com.kylecorry.trail_sense.tools.ruler

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isTrue
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test
import kotlin.math.abs

class ToolRulerTest : ToolTestBase(Tools.RULER) {

    // The measured length depends on where the ruler is tapped and on the screen's density, so the
    // measurements are checked against each other and against the entered map scale instead.

    @Test
    fun measuresInInchesAndCentimeters() {
        click(R.id.ruler)

        hasText(R.id.ruler_unit_btn, "in")
        hasText(R.id.measurement, Regex("\\d+\\.\\d+ in"))
        val inches = measurement()

        click(R.id.ruler_unit_btn)
        hasText(R.id.ruler_unit_btn, "cm")
        hasText(R.id.measurement, Regex("\\d+\\.\\d+ cm"))
        // 1 in = 2.54 cm
        isTrue { abs(measurement() - inches * 2.54) < 0.01 }
    }

    @Test
    fun calculatesTheMapDistanceFromARatio() {
        click(R.id.ruler)
        click(R.id.ruler_unit_btn)
        val centimeters = measurement()

        input(R.id.fractional_map_to, "10")
        hasText(R.id.fractional_map_from, "1")
        hasText(R.id.map_distance, Regex("Map: \\d+\\.\\d+ m"))
        // 1:10 scale, so the map distance is 10 times the measurement (in cm, shown in m)
        isTrue { abs(mapDistance() - centimeters * 10 / 100) < 0.01 }
    }

    @Test
    fun calculatesTheMapDistanceFromAVerbalScale() {
        click(R.id.ruler)
        click(R.id.ruler_unit_btn)
        val centimeters = measurement()
        input(R.id.fractional_map_to, "10")

        click(R.id.map_verbal_btn)
        input(R.id.verbal_map_scale_from, "1")
        input(R.id.verbal_map_scale_to, "10")
        hasText(R.id.map_distance, Regex("Map: \\d+(\\.\\d+)? ft"))
        // 1 in on the map is 10 ft, so the map distance is 10 ft for every 2.54 cm measured
        isTrue { abs(mapDistance() - centimeters / 2.54 * 10) < 0.02 }

        // Switch back to ratio and verify it's still there
        click(R.id.map_ratio_btn)
        hasText(R.id.fractional_map_from, "1")
        hasText(R.id.fractional_map_to, "10")
        hasText(R.id.map_distance, Regex("Map: \\d+\\.\\d+ m"))
    }

    @Test
    fun canMeasureFromTheQuickAction() {
        TestUtils.openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_RULER))
        click(quickAction(Tools.QUICK_ACTION_RULER))
        TestUtils.closeQuickActions()
    }

    private fun measurement(): Double {
        return view(R.id.measurement).uiObject.text.trim().split(" ").first().toDouble()
    }

    private fun mapDistance(): Double {
        return view(R.id.map_distance).uiObject.text.removePrefix("Map:").trim().split(" ").first()
            .toDouble()
    }
}
