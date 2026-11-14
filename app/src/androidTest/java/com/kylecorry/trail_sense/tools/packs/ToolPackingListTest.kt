package com.kylecorry.trail_sense.tools.packs

import androidx.test.uiautomator.Direction
import com.kylecorry.sol.math.MathExtensions.roundPlaces
import com.kylecorry.sol.units.Weight
import com.kylecorry.sol.units.WeightUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.backUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.EXTERNAL_SCREEN_WAIT_FOR_TIMEOUT
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestData
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.clickListItemMenu
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.tools.packs.domain.ItemCategory
import com.kylecorry.trail_sense.tools.packs.domain.PackItem
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Assume.assumeFalse
import org.junit.Test
import kotlin.math.roundToInt

class ToolPackingListTest : ToolTestBase(Tools.PACKING_LISTS) {

    private fun openPack(name: String, vararg items: PackItem) {
        assumeFalse(isStagingBuild)
        relaunchTool { TestData.addPack(name, items.toList()) }
        click(com.kylecorry.andromeda.views.R.id.title)
        hasText(R.id.inventory_list_title, name)
    }

    private fun packItem(
        name: String,
        amount: Int,
        desiredAmount: Int,
        category: ItemCategory,
        weightLbs: Float
    ): PackItem {
        return PackItem(
            0,
            0,
            name,
            category,
            amount.toDouble(),
            desiredAmount.toDouble(),
            Weight.from(weightLbs, WeightUnits.Pounds)
        )
    }

    private fun foodItem(amount: Int) = packItem("Test Item 1", amount, 2, ItemCategory.Food, 1.1f)

    private fun clothingItem(amount: Int) =
        packItem("Test Item 2", amount, 2, ItemCategory.Clothing, 0.5f)

    @Test
    fun canCreateAPack() {
        hasText(R.id.pack_list_title, string(R.string.packing_lists))
        hasText(R.id.empty_text, string(R.string.no_packing_lists))

        createPackingList("Test Pack 1")
    }

    @Test
    fun canCreateAnItem() {
        openPack("Test Pack 1")

        createItem("Test Item 1", 1, 2, string(R.string.category_food), 1.1f)
        hasItem(
            "Test Item 1",
            1,
            2,
            string(R.string.category_food),
            "1.1 lb",
            false
        )
        // 1 of 2 packed, 1 * 1.1 lb
        hasText(R.id.total_percent_packed, "50% packed")
        hasText(R.id.total_packed_weight, "1.1 lb")
    }

    @Test
    fun canCheckAnItem() {
        openPack("Test Pack 1", foodItem(1))

        click(com.kylecorry.andromeda.views.R.id.checkbox)
        // Checking packs the full amount: 2 * 1.1 lb
        hasItem(
            "Test Item 1",
            2,
            2,
            string(R.string.category_food),
            "2.2 lb",
            true
        )
        hasText(R.id.total_percent_packed, "100% packed")
        hasText(R.id.total_packed_weight, "2.2 lb")
    }

    @Test
    fun canEditAnItemOnClick() {
        openPack("Test Pack 1", foodItem(2))

        click(com.kylecorry.andromeda.views.R.id.title)
        hasText(R.id.create_item_title, string(R.string.edit_item_title))
        hasItemForm("Test Item 1", 2, 2, string(R.string.category_food), 1.1f)
        click(R.id.create_btn)
        hasItem(
            "Test Item 1",
            2,
            2,
            string(R.string.category_food),
            "2.2 lb",
            true
        )
    }

    @Test
    fun canEditAnItemFromMenu() {
        openPack("Test Pack 1", foodItem(2))

        clickListItemMenu(string(R.string.edit))
        hasText(R.id.create_item_title, string(R.string.edit_item_title))
        hasItemForm("Test Item 1", 2, 2, string(R.string.category_food), 1.1f)
        click(R.id.create_btn)
        hasItem(
            "Test Item 1",
            2,
            2,
            string(R.string.category_food),
            "2.2 lb",
            true
        )
    }

    @Test
    fun canAddASecondItem() {
        openPack("Test Pack 1", foodItem(2))

        createItem("Test Item 2", 1, 2, string(R.string.category_clothing), 0.5f)
        // The incomplete item is listed first
        hasItem(
            "Test Item 2",
            1,
            2,
            string(R.string.category_clothing),
            "0.5 lb",
            false,
            0
        )
        hasItem(
            "Test Item 1",
            2,
            2,
            string(R.string.category_food),
            "2.2 lb",
            true,
            1
        )
        // (50% + 100%) / 2 items, 2 * 1.1 lb + 1 * 0.5 lb
        hasText(R.id.total_percent_packed, "75% packed")
        hasText(R.id.total_packed_weight, "2.7 lb")
    }

    @Test
    fun canIncrementItem() {
        openPack("Test Pack 1", clothingItem(1))

        clickListItemMenu(string(R.string.add))
        input(string(R.string.dialog_item_amount), "1")
        clickOk()
        hasItem(
            "Test Item 2",
            2,
            2,
            string(R.string.category_clothing),
            "1 lb",
            true
        )
    }

    @Test
    fun canDecrementItem() {
        openPack("Test Pack 1", clothingItem(2))

        clickListItemMenu(string(R.string.subtract))
        input(string(R.string.dialog_item_amount), "2")
        clickOk()
        hasItem(
            "Test Item 2",
            0,
            2,
            string(R.string.category_clothing),
            "0 lb",
            false
        )
    }

    @Test
    fun canDeleteAnItem() {
        openPack("Test Pack 1", clothingItem(0))

        clickListItemMenu(string(R.string.delete))
        hasText(string(R.string.delete_packing_list_item))
        hasText("Test Item 2")
        click(string(android.R.string.cancel))
        hasItem(
            "Test Item 2",
            0,
            2,
            string(R.string.category_clothing),
            "0 lb",
            false
        )

        clickListItemMenu(string(R.string.delete))
        clickOk()
        not {
            hasItem(
                "Test Item 2",
                0,
                2,
                string(R.string.category_clothing),
                "0 lb",
                false
            )
        }
    }

    @Test
    fun canRenameAPack() {
        openPack("Test Pack 1", foodItem(2))

        clickToolbarMenuItem(string(R.string.rename))
        input("Test Pack 1", "Test Pack 2")
        clickOk()
        hasText(R.id.inventory_list_title, "Test Pack 2")

        waitFor {
            TestUtils.back()
        }
        hasText(R.id.pack_list_title, string(R.string.packing_lists))
        hasText(com.kylecorry.andromeda.views.R.id.title, "Test Pack 2")
    }

    @Test
    fun canClearPackAmounts() {
        openPack("Test Pack 1", foodItem(2))

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

    @Test
    fun canExportAPack() {
        openPack("Test Pack 2", foodItem(2))

        clickToolbarMenuItem(string(R.string.export))
        // TODO: Figure out how to click save
//            viewWithText("SAVE").click()
        // Pressing back is needed sometimes to close the drive selector
        backUntil(waitForTime = EXTERNAL_SCREEN_WAIT_FOR_TIMEOUT) {
            hasText("test-pack-2.csv")
        }
        backUntil {
            hasText(R.id.inventory_list_title, "Test Pack 2")
        }
    }

    @Test
    fun canCopyAPack() {
        assumeFalse(isStagingBuild)
        relaunchTool { TestData.addPack("Test Pack 2", listOf(foodItem(1))) }
        hasText(com.kylecorry.andromeda.views.R.id.title, "Test Pack 2")

        clickListItemMenu(string(android.R.string.copy))
        input(string(R.string.name), "Copied Pack")
        clickOk()
        hasText(R.id.inventory_list_title, "Copied Pack")
        // 1 * 1.1 lb
        hasItem(
            "Test Item 1",
            1,
            2,
            string(R.string.category_food),
            "1.1 lb",
            false
        )
    }

    @Test
    fun canDeleteAPack() {
        openPack("Test Pack 1", foodItem(2))

        clickToolbarMenuItem(string(R.string.delete))
        clickOk()
        hasText(R.id.pack_list_title, string(R.string.packing_lists))
        not {
            hasText(com.kylecorry.andromeda.views.R.id.title, "Test Pack 1")
        }
    }

    @Test
    fun canStartImportingAPackingList() {
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

        if (weight % 1 != 0f){
            val pounds = (weight - weight % 1).roundToInt()
            val ounces = (weight - pounds).roundPlaces(1) * 16
            hasText(R.id.item_weight_input){
                it.startsWith(pounds.toString())
            }
            hasText(R.id.item_weight_input){
                it.startsWith(ounces.toString())
            }
        } else {
            hasText(R.id.item_weight_input) {
                it.startsWith(weight.toString())
            }
        }
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
        hasText(com.kylecorry.andromeda.views.R.id.title, name, index = index, exact = true)
        hasText(com.kylecorry.andromeda.views.R.id.tags, category, index = index)
        hasText(
            com.kylecorry.andromeda.views.R.id.data_1,
            "$amount / $desiredAmount",
            index = index,
            exact = true
        )
        hasText(com.kylecorry.andromeda.views.R.id.data_2, weight, index = index, exact = true)
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

        scrollUntil(direction = Direction.UP) {
            click(category)
        }
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
