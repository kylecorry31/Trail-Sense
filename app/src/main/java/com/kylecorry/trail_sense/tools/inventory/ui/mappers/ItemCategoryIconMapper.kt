package com.kylecorry.trail_sense.tools.inventory.ui.mappers

import androidx.annotation.DrawableRes
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.inventory.domain.ItemCategory

class ItemCategoryIconMapper {

    @DrawableRes
    fun getIcon(category: ItemCategory): Int {
        return when (category){
            ItemCategory.Other -> R.drawable.ic_category_other
            ItemCategory.Food -> R.drawable.ic_category_food
            ItemCategory.Hydration -> R.drawable.ic_category_water
            ItemCategory.Tools -> R.drawable.ic_category_tools
            ItemCategory.Natural -> R.drawable.ic_category_natural
            ItemCategory.Clothing -> R.drawable.ic_category_clothing
            ItemCategory.Medical -> R.drawable.ic_category_medical
            ItemCategory.Fire -> R.drawable.ic_category_fire
            ItemCategory.Shelter -> R.drawable.ic_category_shelter
            ItemCategory.Safety -> R.drawable.ic_category_safety
            ItemCategory.Navigation -> R.drawable.ic_category_navigation
            ItemCategory.Electronics -> R.drawable.ic_sensors
            ItemCategory.Documents -> R.drawable.ic_file
            ItemCategory.Hygiene -> R.drawable.ic_hygiene
        }
    }

}