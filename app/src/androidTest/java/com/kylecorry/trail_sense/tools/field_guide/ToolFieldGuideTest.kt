package com.kylecorry.trail_sense.tools.field_guide

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollToEnd
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollToStart
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.clickListItemMenu
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolFieldGuideTest : ToolTestBase(Tools.FIELD_GUIDE) {

    @Test
    fun verifyBasicFunctionality() {
        // Disclaimer
        clickOk()

        hasCategories()
        canOpenPage()
        canCreatePage()
        canEditPage()
        canDeletePage()
        canSearch()
    }

    private fun canSearch() {
        back(false)
        input(R.id.search, "Rabbit")
        hasText("Rabbit")

        input(R.id.search, "Insect")
        hasText("Ant")

        back(false)
        hasText("Plant")
    }

    private fun hasCategories() {
        // Shows categories
        val categories = listOf(
            "Plant",
            "Fungus",
            "Animal",
            "Mammal",
            "Bird",
            "Reptile",
            "Amphibian",
            "Fish",
            "Invertebrate",
            "Rock",
            "Other"
        )

        for (category in categories) {
            scrollUntil {
                hasText(category, waitForTime = 500)
            }
        }

        // Open a section
        scrollToStart(R.id.list)
        click("Animal")
        hasText("Ant")
        hasText("Clam")
    }

    private fun canDeletePage() {
        clickListItemMenu("Delete")
        clickOk()
        not { hasText("A Test 3", waitForTime = 0) }
    }

    private fun canEditPage() {
        clickListItemMenu("Edit")
        hasText(R.id.name, "A Test", contains = true)
        input(R.id.name, "A Test 2")
        click(toolbarButton(R.id.create_field_guide_page_title, Side.Right))

        // Verify the changes
        hasText("A Test 2")

        // Open it an edit
        click("A Test 2")
        click(toolbarButton(R.id.field_guide_page_title, Side.Right))
        hasText(R.id.name, "A Test 2", contains = true)
        input(R.id.name, "A Test 3")
        click(toolbarButton(R.id.create_field_guide_page_title, Side.Right))

        // Verify the changes
        hasText(R.id.field_guide_page_title, "A Test 3")

        back()
    }

    private fun canOpenPage() {
        click("Ant")
        hasText(R.id.field_guide_page_title, "Ant")
        hasText("A small insect", contains = true)
        val tags = listOf(
            "North America",
            "Insect",
            "Grassland",
        )
        for (tag in tags) {
            scrollUntil {
                hasText(tag, waitForTime = 0)
            }
        }
        back()
    }

    private fun canCreatePage() {
        click(R.id.add_btn)
        input(R.id.name, "A Test")
        click(R.id.tag_classifications)
        click("Bird")
        clickOk()
        hasText(R.id.tag_classifications, "Animal, Bird", contains = true)
        input(R.id.notes, "Notes")
        scrollToEnd(R.id.scroll_view)

        click(R.id.tag_locations)
        click("North America")
        clickOk()
        hasText(R.id.tag_locations, "North America", contains = true)

        click(R.id.tag_habitats)
        click("Forest")
        clickOk()
        hasText(R.id.tag_habitats, "Forest", contains = true)

        click(R.id.tag_human_interactions)
        click("Edible")
        click("Inedible")
        clickOk()
        hasText(R.id.tag_human_interactions, "Edible, Inedible", contains = true)

        click(R.id.tag_activity_patterns)
        click("Diurnal (day)")
        clickOk()
        hasText(R.id.tag_activity_patterns, "Diurnal (day)", contains = true)

        scrollToStart(R.id.scroll_view)
        click("Take photo")
        click(R.id.capture_button)

        isVisible(R.id.image)

        click(toolbarButton(R.id.create_field_guide_page_title, Side.Right))

        // Verify it shows up in the list
        hasText("A Test")
        click("A Test")

        // Verify the details
        hasText(R.id.field_guide_page_title, "A Test")
        hasText("Notes")

        val tags = listOf(
            "Animal",
            "Bird",
            "North America",
            "Forest",
            "Edible",
            "Inedible",
            "Diurnal (day)"
        )

        for (tag in tags) {
            scrollUntil {
                hasText(tag, waitForTime = 0)
            }
        }

        back()
    }
}