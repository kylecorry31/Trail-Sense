package com.kylecorry.trail_sense.tools.guide

import androidx.annotation.RawRes
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.openQuickActions
import com.kylecorry.trail_sense.test_utils.TestUtils.rawResourceText
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolUserGuideTest : ToolTestBase(Tools.USER_GUIDE) {

    @Test
    fun canOpenAGuide() {
        isVisible(R.id.searchbox)

        // The general guide is listed first
        hasText(string(R.string.general))

        click(string(R.string.tools), exact = true)
        hasGuide(string(R.string.tools), R.raw.guide_tool_tools)
        back()

        // Tool guides are listed below the general guide
        scrollUntil { click(string(R.string.tool_battery_title), exact = true) }
        hasGuide(string(R.string.tool_battery_title), R.raw.guide_tool_battery)
    }

    @Test
    fun canSearchTheGuides() {
        hasText(string(R.string.general))

        input(R.id.search_view_edit_text, "Sett")
        hasText("Settings")

        // The general category no longer matches
        not { hasText(string(R.string.general)) }
    }

    @Test
    fun canOpenTheUserGuideFromTheQuickAction() {
        openQuickActions()

        click(quickAction(Tools.QUICK_ACTION_USER_GUIDE))

        // Verify it shows the user guide
        hasGuide(string(R.string.tools), R.raw.guide_tool_tools)
    }

    private fun hasGuide(name: String, @RawRes contents: Int) {
        // The first paragraph is plain text, so it is shown as written in the guide
        val firstParagraph = rawResourceText(contents).lines()
            .first { it.isNotBlank() && !it.startsWith("<!--") }
            .trim()
        hasText(R.id.guide_name, name)
        hasText(R.id.guide_scroll) { it.contains(firstParagraph) }
    }
}
