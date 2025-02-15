package com.kylecorry.trail_sense.tools.ruler

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolRulerTest : ToolTestBase(Tools.RULER) {
    @Test
    fun verifyBasicFunctionality() {
        click(R.id.ruler)

        // Verify amount
        hasText(R.id.ruler_unit_btn, "in")
        hasText(R.id.measurement, Regex("\\d+\\.\\d+ in"))

        // Switch to CM
        click(R.id.ruler_unit_btn)
        hasText(R.id.ruler_unit_btn, "cm")
        hasText(R.id.measurement, Regex("\\d+\\.\\d+ cm"))

        // Enter a to ratio
        input(R.id.fractional_map_to, "10")
        hasText(R.id.fractional_map_from, "1", contains = true)
        hasText(R.id.map_distance, Regex("Map: \\d+\\.\\d+ m"))

        // Switch to verbal scale
        click(R.id.map_verbal_btn)
        input(R.id.verbal_map_scale_from, "1")
        input(R.id.verbal_map_scale_to, "10")
        hasText(R.id.map_distance, Regex("Map: \\d+\\.\\d+ ft"))

        // Switch back to ratio and verify it's still there
        click(R.id.map_ratio_btn)
        hasText(R.id.fractional_map_from, "1", contains = true)
        hasText(R.id.fractional_map_to, "10", contains = true)
        hasText(R.id.map_distance, Regex("Map: \\d+\\.\\d+ m"))

        verifyQuickActions()
    }

    private fun verifyQuickActions() {
        TestUtils.openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_RULER))
        click(quickAction(Tools.QUICK_ACTION_RULER))
        TestUtils.closeQuickActions()
    }
}