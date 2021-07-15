package com.kylecorry.trail_sense.tools.inventory.ui.mappers

import com.kylecorry.trail_sense.shared.AppColor
import com.kylecorry.trail_sense.tools.inventory.domain.ItemCategory

class ItemCategoryColorMapper {

    fun map(category: ItemCategory): AppColor {
        return when (category){
            ItemCategory.Other -> AppColor.Orange // TODO: Gray
            ItemCategory.Food -> AppColor.Green
            ItemCategory.Hydration -> AppColor.Blue
            ItemCategory.Tools -> AppColor.Red // TODO: Gray
            ItemCategory.Natural -> AppColor.Green
            ItemCategory.Clothing -> AppColor.Purple
            ItemCategory.Medical -> AppColor.Red
            ItemCategory.Fire -> AppColor.Orange
            ItemCategory.Shelter -> AppColor.Purple // TODO: Brown
            ItemCategory.Safety -> AppColor.Yellow
            ItemCategory.Navigation -> AppColor.Yellow
            ItemCategory.Electronics -> AppColor.Green // TODO: Unknown
            ItemCategory.Documents -> AppColor.Blue // TODO: Unknown
            ItemCategory.Hygiene -> AppColor.Red // TODO: White (which won't work on dark)
        }
    }

}