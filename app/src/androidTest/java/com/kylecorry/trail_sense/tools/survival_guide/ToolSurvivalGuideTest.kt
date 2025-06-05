package com.kylecorry.trail_sense.tools.survival_guide

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.tools.survival_guide.domain.Chapters
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolSurvivalGuideTest : ToolTestBase(Tools.SURVIVAL_GUIDE) {
    @Test
    fun verifyBasicFunctionality() {
        // Accept the disclaimer
        clickOk()

        isVisible(R.id.list)

        // Verify it shows each chapter
        val chapters = Chapters.getChapters(context)
        chapters.forEachIndexed { index, chapter ->
            scrollUntil {
                hasText(chapter.title)
            }

            click(chapter.title)

            // Wait for the chapter to load
            hasText(R.id.guide_name, chapter.title, contains = true, ignoreCase = true)
            hasText(R.id.guide_scroll) { it.isNotEmpty() }
            click("Be prepared")

            back()
        }

        // Search
        input(R.id.search, "Eating fish")
        hasText("fishing gear", contains = true)
        click("Fish")

        hasText(R.id.guide_name, "Food")
        hasText(R.id.guide_scroll) { it.contains("Fish") }

        back()

        verifyQuickAction()
    }

    private fun verifyQuickAction() {
        TestUtils.openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_SURVIVAL_GUIDE))

        input(R.id.search, "Eating fish", closeKeyboardOnCompletion = true)
        hasText("fishing gear", contains = true)
        click("Fish")

        hasText("Food")

        back()

        TestUtils.openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_SURVIVAL_GUIDE))

        input(R.id.search, "Eating fish", closeKeyboardOnCompletion = true)

        // Continue search on survival guide list
        click(toolbarButton(R.id.title, Side.Right))

        isVisible(R.id.list)
        hasText(R.id.search, "Eating fish", contains = true)
    }
}