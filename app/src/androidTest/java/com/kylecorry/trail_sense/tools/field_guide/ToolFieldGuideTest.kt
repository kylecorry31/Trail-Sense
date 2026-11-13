package com.kylecorry.trail_sense.tools.field_guide

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.GPS_WAIT_FOR_TIMEOUT
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.backUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollToEnd
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollToStart
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.test_utils.TestData
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.clickListItemMenu
import com.kylecorry.trail_sense.test_utils.TestUtils.pickDate
import com.kylecorry.trail_sense.test_utils.TestUtils.pickTime
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTag
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Assume.assumeFalse
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ToolFieldGuideTest : ToolTestBase(Tools.FIELD_GUIDE) {

    private fun openWithAPage() {
        assumeFalse(isStagingBuild)
        relaunchTool {
            TestData.addFieldGuidePage("A Test", listOf(FieldGuidePageTag.Bird), "Notes")
        }
        // Disclaimer
        clickOk()
    }

    private fun openCategoryWithTestPage() {
        click("Animal")
        hasText("A Test")
    }

    @Test
    fun canBrowseSearchAndHideBuiltInPages() {
        // Disclaimer
        clickOk()

        // Shows categories
        for (category in listOf("Plant", "Other")) {
            scrollUntil {
                hasText(category)
            }
        }

        // Open a section
        scrollToStart(R.id.list)
        click("Animal")
        hasText("Ant")
        hasText("Clam")

        // Open a built-in page
        click("Ant")
        hasText(R.id.field_guide_page_title, "Ant")
        hasText("A small insect")
        scrollUntil {
            hasText("Grassland")
        }
        back()

        // Search
        back(false)
        input(R.id.search, "Rabbit")
        hasText("Rabbit")

        input(R.id.search, "Insect")
        hasText("Ant")

        back(false)
        hasText("Plant")

        // Hide a built-in page
        click("Animal")
        hasText("Ant")
        clickListItemMenu("Hide")
        clickOk()
        not { hasText("Ant") }
    }

    @Test
    fun canCreateAPage() {
        // Disclaimer
        clickOk()

        // Creating a page from a category preselects its classification
        click("Animal")
        click(R.id.add_btn)
        input(R.id.name, "A Test")
        click(R.id.tag_classifications)
        click("Bird")
        clickOk()
        hasText(R.id.tag_classifications, "Animal, Bird")
        scrollUntil { input(R.id.notes, "Notes") }
        scrollToEnd(R.id.scroll_view)

        click(R.id.tag_locations)
        click("North America")
        clickOk()
        hasText(R.id.tag_locations, "North America")

        click(R.id.tag_habitats)
        click("Forest")
        clickOk()
        hasText(R.id.tag_habitats, "Forest")

        click(R.id.tag_human_interactions)
        click("Edible")
        click("Inedible")
        clickOk()
        hasText(R.id.tag_human_interactions, "Edible, Inedible")

        click(R.id.tag_activity_patterns)
        click("Diurnal (day)")
        clickOk()
        hasText(R.id.tag_activity_patterns, "Diurnal (day)")

        scrollToStart(R.id.scroll_view)
        click("Take photo")
        click(R.id.capture_button)

        isVisible(R.id.photo_upload_image)

        click(toolbarButton(R.id.create_field_guide_page_title, Side.Right))

        // Verify it shows up in the list
        hasText("A Test")
        click("A Test")

        // Verify the details
        hasText(R.id.field_guide_page_title, "A Test")
        hasText("Notes")

        for (tag in listOf("Bird", "Diurnal (day)")) {
            scrollUntil {
                hasText(tag)
            }
        }
    }

    @Test
    fun canEditAndDeleteAPage() {
        openWithAPage()
        openCategoryWithTestPage()

        clickListItemMenu("Edit")
        hasText(R.id.name, "A Test")
        input(R.id.name, "A Test 2")
        click(toolbarButton(R.id.create_field_guide_page_title, Side.Right))

        // Verify the changes
        hasText("A Test 2")

        // Open it an edit
        click("A Test 2")
        click(toolbarButton(R.id.field_guide_page_title, Side.Right))
        hasText(R.id.name, "A Test 2")
        input(R.id.name, "A Test 3")
        click(toolbarButton(R.id.create_field_guide_page_title, Side.Right))

        // Verify the changes
        hasText(R.id.field_guide_page_title, "A Test 3")

        back()

        clickListItemMenu("Delete")
        clickOk()
        not { hasText("A Test 3") }
    }

    @Test
    fun canManageMultipleImages() {
        openWithAPage()
        openCategoryWithTestPage()

        clickListItemMenu("Edit")

        // The first slot is used to add an image
        takePhoto()
        scrollUntil(R.id.scroll_view) { hasText(R.id.photo_position, "Image 1/1") }

        // The slot after the last image is used to add another one
        click(R.id.next_photo_button)
        scrollToStart(R.id.scroll_view)
        takePhoto()
        hasText(R.id.photo_position, "Image 2/2")

        // The arrows page through the images
        click(R.id.previous_photo_button)
        hasText(R.id.photo_position, "Image 1/2")
        click(R.id.next_photo_button)
        hasText(R.id.photo_position, "Image 2/2")

        // Setting an image as the default moves it to the first slot
        click(R.id.photo_upload_menu_button)
        click("Set as default")
        hasText(R.id.photo_position, "Image 1/2")

        click(toolbarButton(R.id.create_field_guide_page_title, Side.Right))

        // The page displays all the images
        click("A Test")
        hasText(R.id.photo_position, "Image 1/2")
        click(R.id.next_photo_button)
        hasText(R.id.photo_position, "Image 2/2")
        back()

        // An image can be deleted
        clickListItemMenu("Edit")
        scrollUntil(R.id.scroll_view) { hasText(R.id.photo_position, "Image 1/2") }
        click(R.id.photo_upload_menu_button)
        click("Delete")
        clickOk()
        hasText(R.id.photo_position, "Image 1/1")

        click(toolbarButton(R.id.create_field_guide_page_title, Side.Right))
    }

    private fun takePhoto() {
        scrollUntil(R.id.scroll_view) { click("Take photo") }
        waitFor {
            // The click can land before the list settles, so repeat it until the camera is open
            try {
                isVisible(R.id.capture_button, waitForTime = 1)
            } catch (e: Throwable) {
                click("Take photo", waitForTime = 1)
                throw e
            }
        }
        click(R.id.capture_button)
    }

    @Test
    fun canRecordSightingWithQuickAction() {
        openWithAPage()

        TestUtils.openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_RECORD_SIGHTING))
        input(R.id.search, "A Test")
        click("A Test", exact = true, index = 1)

        hasText("Created", waitForTime = GPS_WAIT_FOR_TIMEOUT)
        click("View")
        input("Notes", "Quick action sighting")
        click(toolbarButton(R.id.title, Side.Right))

        // The quick action deep link returns to the sightings list after saving.
        hasText("Quick action sighting")
        back()
        hasText("Sightings (1)")
        back()
    }

    @Test
    fun canLogAndEditSightings() {
        // Disclaimer
        clickOk()

        click("Animal")
        click("Ant")
        click("Sightings (0)")
        hasText("No sightings")
        hasText(R.id.toolbar_subtitle, "Ant", exact = true)
        click(R.id.create_btn)
        hasText(R.id.toolbar_subtitle, "Ant", exact = true)
        click(R.id.time)
        pickDate(2025, 1, 1)
        pickTime(6, 0, true)
        hasText("January 1, 2025 6:00 AM")
        input(R.id.location, "42, 72")
        input(R.id.elevation, "100")
        click("Harvested")
        click("Show on map")
        input("Notes", "Test")
        click(toolbarButton(R.id.title, Side.Right))

        // Sightings list
        hasText("Sightings")
        hasText("January 1, 2025 6:00 AM")
        hasText("Test")

        // Edit by clicking
        click("January 1, 2025 6:00 AM")
        hasText("January 1, 2025 6:00 AM")
        hasText("42.000000°,  72.000000°")
        hasText("100")
        isChecked("Harvested")
        isNotChecked("Show on map")
        click("Show on map")
        input("Test", "Test 2")
        click(toolbarButton(R.id.title, Side.Right))

        // Sightings list is updated
        hasText("Test 2")

        // Edit via menu
        clickListItemMenu("Edit")
        hasText("January 1, 2025 6:00 AM")
        hasText("42.000000°,  72.000000°")
        hasText("100")
        isChecked("Harvested")
        isChecked("Show on map")
        input("Test 2", "Test 3")
        click(toolbarButton(R.id.title, Side.Right))

        // Sightings list is updated
        hasText("Test 3")

        // Count on field guide page
        backUntil {
            click("Sightings (1)")
        }
    }

    @Test
    fun canUseAndDeleteASighting() {
        assumeFalse(isStagingBuild)
        relaunchTool {
            TestData.addFieldGuideSighting(
                TestData.builtInFieldGuidePageId("Ant"),
                time = LocalDateTime.of(2025, 1, 1, 6, 0).atZone(ZoneId.systemDefault()).toInstant(),
                location = Coordinate(42.0, 72.0),
                altitudeMeters = 30.48f,
                harvested = true,
                notes = "Test",
                showOnMap = false
            )
        }
        clickOk()

        click("Animal")
        click("Ant")
        click("Sightings (1)")

        // Navigate
        clickListItemMenu("Navigate")
        hasText(Regex("\\s*\\d+°\\s+[NSEW]+"))
        hasText("Ant")
        back()

        // Create beacon
        clickListItemMenu("Create beacon")
        hasText("Ant")
        hasText("42.000000°,  72.000000°")
        hasText("100")
        back()
        click("Leave")

        backUntil {
            click("Sightings (1)")
        }

        // Delete from the list
        clickListItemMenu("Delete")
        clickOk()

        hasText("No sightings")

        // Create another sighting to delete from the edit page
        click(R.id.create_btn)
        input("Notes", "Delete from edit page")
        click(toolbarButton(R.id.title, Side.Right))
        hasText("Delete from edit page")

        // Delete from the edit page
        click("Delete from edit page")
        click(toolbarButton(R.id.title, Side.Left))
        click("Delete")
        clickOk()

        hasText("No sightings")
        back()
        hasText("Sightings (0)")
    }
}
