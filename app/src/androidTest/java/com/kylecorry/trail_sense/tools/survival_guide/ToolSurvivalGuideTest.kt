package com.kylecorry.trail_sense.tools.survival_guide

import androidx.annotation.RawRes
import androidx.annotation.StringRes
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.rawResourceText
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolSurvivalGuideTest : ToolTestBase(Tools.SURVIVAL_GUIDE) {

    // Each chapter is listed with the summary from its guide
    private val chapters = listOf(
        R.string.overview to R.raw.guide_survival_chapter_overview,
        R.string.category_medical to R.raw.guide_survival_chapter_medical,
        R.string.shelter to R.raw.guide_survival_chapter_shelter,
        R.string.water to R.raw.guide_survival_chapter_water,
        R.string.category_fire to R.raw.guide_survival_chapter_fire,
        R.string.category_food to R.raw.guide_survival_chapter_food,
        R.string.navigation to R.raw.guide_survival_chapter_navigation,
        R.string.weather to R.raw.guide_survival_chapter_weather,
        R.string.references to R.raw.guide_survival_chapter_references
    )

    @Test
    fun listsTheChapters() {
        // Accept the disclaimer
        clickOk()

        isVisible(R.id.list)

        chapters.forEach { (title, contents) ->
            scrollUntil {
                hasText(string(title), exact = true)
                summary(contents)?.let { hasText(it) }
            }
        }
    }

    @Test
    fun canOpenTheFirstAndLastChapters() {
        // Accept the disclaimer
        clickOk()

        isVisible(R.id.list)

        openChapter(R.string.overview, R.raw.guide_survival_chapter_overview)
        scrollUntil { click("Be prepared") }
        back()

        openChapter(R.string.references, R.raw.guide_survival_chapter_references)
        scrollUntil { click("Books and manuals") }
        back()
    }

    @Test
    fun canSearchTheChapters() {
        clickOk()

        input(R.id.search, "Catching fish")
        hasText("fishing gear")
        click("Fish")

        hasText(R.id.guide_name, "Food")
        hasText(R.id.guide_scroll) { it.contains("Fish") }
    }

    @Test
    fun canSearchFromTheQuickActionAndContinueOnTheChapterList() {
        clickOk()

        TestUtils.openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_SURVIVAL_GUIDE))

        input(R.id.search, "Catching fish", closeKeyboardOnCompletion = true)
        hasText("Fish are most active")
        click("Fish > Fishing")

        hasText("Food")

        back()

        TestUtils.openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_SURVIVAL_GUIDE))

        input(R.id.search, "Catching fish", closeKeyboardOnCompletion = true)

        // Continue search on survival guide list
        click(toolbarButton(R.id.title, Side.Right))

        isVisible(R.id.list)
        hasText(R.id.search, "Catching fish")
    }

    private fun summary(@RawRes contents: Int): String? {
        return Regex("<!-- S: (.*) -->").find(rawResourceText(contents))?.groupValues?.get(1)
    }

    private fun openChapter(@StringRes title: Int, @RawRes contents: Int) {
        scrollUntil {
            click(string(title), exact = true)
        }

        // Wait for the chapter to load
        hasText(R.id.guide_name, string(title), ignoreCase = true)
        // The first paragraph is plain text, so it is shown as written in the guide
        val firstParagraph = rawResourceText(contents).lines()
            .first { it.isNotBlank() && !it.startsWith("<!--") }
            .trim()
        hasText(R.id.guide_scroll) { it.contains(firstParagraph) }
    }
}
