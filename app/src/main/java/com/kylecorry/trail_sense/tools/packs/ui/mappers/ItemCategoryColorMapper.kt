package com.kylecorry.trail_sense.tools.packs.ui.mappers

import com.kylecorry.trail_sense.shared.AppColor
import com.kylecorry.trail_sense.tools.packs.domain.ItemCategory

class ItemCategoryColorMapper {

    fun map(category: ItemCategory): AppColor {
        return when (category){
            ItemCategory.Other -> AppColor.Gray
            ItemCategory.Food -> AppColor.Green
            ItemCategory.Hydration -> AppColor.Blue
            ItemCategory.Tools -> AppColor.Yellow
            ItemCategory.Natural -> AppColor.Green
            ItemCategory.Clothing -> AppColor.Purple
            ItemCategory.Medical -> AppColor.Red
            ItemCategory.Fire -> AppColor.Orange
            ItemCategory.Shelter -> AppColor.Brown
            ItemCategory.Safety -> AppColor.Yellow
            ItemCategory.Navigation -> AppColor.Yellow
            ItemCategory.Electronics -> AppColor.Green
            ItemCategory.Documents -> AppColor.Blue
            ItemCategory.Hygiene -> AppColor.Gray
        }
    }

}