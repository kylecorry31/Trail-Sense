package com.kylecorry.trail_sense.tools.packs

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.clickListItemMenu
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolPackingListTest : ToolTestBase(Tools.PACKING_LISTS) {

    @Test
    fun verifyBasicFunctionality() {
        hasText(R.id.pack_list_title, string(R.string.packing_lists))

        shouldHaveNoPacks()
        canCreateAPack()
        canCreateAnItem()
        canCheckAnItem()
        canEditAnItemOnClick()
        canAddASecondItem()

        // Item menu items
        canIncrementItem()
        canDecrementItem()
        canEditAnItemFromMenu()
        canDeleteAnItem()

        // Pack menu items
        canRenamePack()
        canClearPackAmounts()
        canExportPack()
        // TODO: Sort items

        // Pack list
        canShowPackList()
        canCopyPack()
        canDeletePack()
        canImportPack()
    }

    private fun shouldHaveNoPacks() {
        hasText(R.id.empty_text, string(R.string.no_packing_lists))
    }

    private fun canCreateAPack() {
        createPackingList("Test Pack 1")
    }

    private fun canCreateAnItem() {
        createItem("Test Item 1", 1, 2, string(R.string.category_food), 1.1f)
        hasItem(
            "Test Item 1",
            1,
            2,
            string(R.string.category_food),
            "1 lb",
            false
        )
        hasText(R.id.total_percent_packed, "50% packed")
        hasText(R.id.total_packed_weight, "1.1 lb")
    }

    private fun canCheckAnItem() {
        click(com.kylecorry.andromeda.views.R.id.checkbox)
        hasItem(
            "Test Item 1",
            2,
            2,
            string(R.string.category_food),
            "2 lb",
            true
        )
        hasText(R.id.total_percent_packed, "100% packed")
        hasText(R.id.total_packed_weight, "2.2 lb")
    }

    private fun canEditAnItemOnClick() {
        click(com.kylecorry.andromeda.views.R.id.title)
        hasText(R.id.create_item_title, string(R.string.edit_item_title))
        hasItemForm("Test Item 1", 2, 2, string(R.string.category_food), 1.1f)
        click(R.id.create_btn)
        hasItem(
            "Test Item 1",
            2,
            2,
            string(R.string.category_food),
            "2 lb",
            true
        )
    }

    private fun canEditAnItemFromMenu() {
        clickListItemMenu(string(R.string.edit), 1)
        hasText(R.id.create_item_title, string(R.string.edit_item_title))
        hasItemForm("Test Item 1", 2, 2, string(R.string.category_food), 1.1f)
        click(R.id.create_btn)
        hasItem(
            "Test Item 1",
            2,
            2,
            string(R.string.category_food),
            "2 lb",
            true,
            1
        )
    }

    private fun canAddASecondItem() {
        createItem("Test Item 2", 1, 2, string(R.string.category_clothing), 0.5f)
        hasItem(
            "Test Item 2",
            1,
            2,
            string(R.string.category_clothing),
            "1 lb",
            false,
            0
        )
        hasItem(
            "Test Item 1",
            2,
            2,
            string(R.string.category_food),
            "2 lb",
            true,
            1
        )
        hasText(R.id.total_percent_packed, "75% packed")
        hasText(R.id.total_packed_weight, "2.7 lb")
    }

    private fun canIncrementItem() {
        clickListItemMenu(string(R.string.add), 0)
        input(string(R.string.dialog_item_amount), "1")
        clickOk()
        hasItem(
            "Test Item 2",
            2,
            2,
            string(R.string.category_clothing),
            "1 lb",
            true,
            0
        )
    }

    private fun canDecrementItem() {
        clickListItemMenu(string(R.string.subtract), 0)
        input(string(R.string.dialog_item_amount), "2")
        clickOk()
        hasItem(
            "Test Item 2",
            0,
            2,
            string(R.string.category_clothing),
            "0 lb",
            false,
            0
        )
    }

    private fun canDeleteAnItem() {
        clickListItemMenu(string(R.string.delete), 0)
        not {
            hasItem(
                "Test Item 2",
                0,
                2,
                string(R.string.category_clothing),
                "0 lb",
                false,
                0
            )
        }
    }

    private fun canRenamePack() {
        clickToolbarMenuItem(string(R.string.rename))
        input("Test Pack 1", "Test Pack 2")
        clickOk()
        hasText(R.id.inventory_list_title, "Test Pack 2")
    }

    private fun canClearPackAmounts() {
        clickToolbarMenuItem(string(R.string.clear_amounts))
        clickOk()
        hasItem(
            "Test Item 1",
            0,
            2,
            string(R.string.category_food),
            "0 lb",
            false
        )
    }

    private fun canExportPack() {
        clickToolbarMenuItem(string(R.string.export))
        // TODO: Figure out how to click save
//            viewWithText("SAVE").click()
        hasText("test-pack-2.csv")
        waitFor {
            waitFor {
                TestUtils.back()
            }
            hasText(R.id.inventory_list_title, "Test Pack 2")
        }
    }

    private fun canShowPackList() {
        waitFor {
            TestUtils.back()
        }
        hasText(R.id.pack_list_title, string(R.string.packing_lists))
        hasText(com.kylecorry.andromeda.views.R.id.title, "Test Pack 2")
    }

    private fun canCopyPack() {
        clickListItemMenu(string(android.R.string.copy))
        input(string(R.string.name), "Copied Pack")
        clickOk()
        hasText(R.id.inventory_list_title, "Copied Pack")
        hasItem(
            "Test Item 1",
            0,
            2,
            string(R.string.category_food),
            "0 lb",
            false
        )
    }

    private fun canDeletePack() {
        clickToolbarMenuItem(string(R.string.delete))
        clickOk()
        hasText(R.id.pack_list_title, string(R.string.packing_lists))
        not {
            hasText(com.kylecorry.andromeda.views.R.id.title, "Copied Pack")
        }
    }

    private fun canImportPack() {
        click(R.id.add_btn)
        click(string(R.string.import_packing_list))
        // TODO: Actually import a pack and verify it
    }

    // HELPERS
    private fun hasItemForm(
        name: String,
        amount: Int,
        desiredAmount: Int,
        category: String,
        weight: Float
    ) {
        hasText(R.id.name_edit) { it.startsWith(name) }
        hasText(R.id.count_edit) { it.startsWith(amount.toString()) }
        hasText(R.id.desired_amount_edit) { it.startsWith(desiredAmount.toString()) }
        hasText(R.id.category_spinner) { it.startsWith(category) }
        hasText(R.id.item_weight_input) { it.startsWith(weight.toString()) }
    }

    private fun hasItem(
        name: String,
        amount: Int,
        desiredAmount: Int,
        category: String,
        weight: String,
        isChecked: Boolean,
        index: Int = 0
    ) {
        hasText(com.kylecorry.andromeda.views.R.id.title, name, index = index)
        hasText(com.kylecorry.andromeda.views.R.id.tags, category, index = index)
        hasText(
            com.kylecorry.andromeda.views.R.id.data_1,
            "$amount / $desiredAmount",
            index = index
        )
        hasText(com.kylecorry.andromeda.views.R.id.data_2, weight, index = index)
        isChecked(com.kylecorry.andromeda.views.R.id.checkbox, isChecked, index = index)
    }

    private fun clickToolbarMenuItem(label: String) {
        click(toolbarButton(R.id.inventory_list_title, Side.Right))
        click(label)
    }

    private fun createItem(
        name: String,
        amount: Int,
        desiredAmount: Int,
        category: String,
        weight: Float
    ) {
        click(R.id.add_btn)
        hasText(R.id.create_item_title, string(R.string.create_item_title))

        input(R.id.name_edit, name)
        input(R.id.count_edit, amount.toString())
        input(R.id.desired_amount_edit, desiredAmount.toString())
        click(R.id.category_spinner)
        click(category)
        clickOk()
        input(R.id.item_weight_input, weight.toString())
        click(R.id.create_btn)
    }

    private fun createPackingList(name: String) {
        // Create a new packing list
        click(R.id.add_btn)
        click(string(R.string.new_packing_list))
        input(string(R.string.name), name)
        clickOk()

        // Verify the new pack is created
        hasText(R.id.inventory_list_title, name)
        hasText(R.id.inventory_empty_text, string(R.string.inventory_empty_text))
    }
}