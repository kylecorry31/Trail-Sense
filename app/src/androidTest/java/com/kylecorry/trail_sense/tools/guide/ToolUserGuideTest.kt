package com.kylecorry.trail_sense.tools.guide

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.TestUtils.openQuickActions
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.tools.guide.infrastructure.Guides
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolUserGuideTest : ToolTestBase(Tools.USER_GUIDE) {

    @Test
    fun verifyBasicFunctionality() {
        isVisible(R.id.searchbox)

        // Verify it shows each guide
        val guides = Guides.guides(context)
            .flatMap { it.guides }
            // TODO: Open each guide, maybe by navigating directly to them
            .take(2)


        guides.forEachIndexed { index, guide ->

            // Skip the weather guide (no good way to differentiate it from the section in this test)
            if (guide.name == "Weather") {
                return@forEachIndexed
            }

            // Wait for the guides to load
            hasText(guides.first().name)

            click(guide.name)

            // Wait for the guide to load
            hasText(R.id.guide_name, guide.name)
            hasText(R.id.guide_scroll) { it.isNotEmpty() }

            back()
        }

        // Search
        input(R.id.search_view_edit_text, "Sett")
        hasText("Settings")

        verifyQuickAction()
    }

    fun verifyQuickAction() {
        openQuickActions()

        click(quickAction(Tools.QUICK_ACTION_USER_GUIDE))

        // Verify it shows the user guide
        hasText(R.id.guide_name, "Tools")
        hasText(R.id.guide_scroll) { it.isNotEmpty() }
    }
}