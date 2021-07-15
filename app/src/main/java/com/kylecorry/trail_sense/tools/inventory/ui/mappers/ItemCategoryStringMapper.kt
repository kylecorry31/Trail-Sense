package com.kylecorry.trail_sense.tools.inventory.ui.mappers

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.inventory.domain.ItemCategory

class ItemCategoryStringMapper(private val context: Context) {

    fun getString(category: ItemCategory): String {
        return when (category) {
            ItemCategory.Other -> context.getString(R.string.category_other)
            ItemCategory.Food -> context.getString(R.string.category_food)
            ItemCategory.Hydration -> context.getString(R.string.category_hydration)
            ItemCategory.Tools -> context.getString(R.string.tools)
            ItemCategory.Clothing -> context.getString(R.string.category_clothing)
            ItemCategory.Medical -> context.getString(R.string.category_medical)
            ItemCategory.Fire -> context.getString(R.string.category_fire)
            ItemCategory.Shelter -> context.getString(R.string.category_shelter)
            ItemCategory.Safety -> context.getString(R.string.category_safety)
            ItemCategory.Natural -> context.getString(R.string.category_natural)
            ItemCategory.Navigation -> context.getString(R.string.navigation)
            ItemCategory.Electronics -> context.getString(R.string.electronics)
            ItemCategory.Documents -> context.getString(R.string.documents)
            ItemCategory.Hygiene -> context.getString(R.string.hygiene)
        }
    }

}