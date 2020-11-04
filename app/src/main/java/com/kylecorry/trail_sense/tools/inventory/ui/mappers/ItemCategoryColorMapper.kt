package com.kylecorry.trail_sense.tools.inventory.ui.mappers

import androidx.annotation.ColorRes
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.inventory.domain.ItemCategory

class ItemCategoryColorMapper {

    @ColorRes
    fun map(category: ItemCategory): Int {
        return when (category){
            ItemCategory.Other -> R.color.category_other
            ItemCategory.Food -> R.color.category_food
            ItemCategory.Hydration -> R.color.category_water
            ItemCategory.Tools -> R.color.category_tools
            ItemCategory.Natural -> R.color.category_natural
            ItemCategory.Clothing -> R.color.category_clothing
            ItemCategory.Medical -> R.color.category_medical
            ItemCategory.Fire -> R.color.category_fire
            ItemCategory.Shelter -> R.color.category_shelter
            ItemCategory.Safety -> R.color.category_safety
            ItemCategory.Navigation -> R.color.category_navigation
        }
    }

}