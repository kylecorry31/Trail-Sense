package com.kylecorry.trail_sense.tools.packs.domain.sort

import com.kylecorry.andromeda.core.units.WeightUnits
import com.kylecorry.trail_sense.tools.packs.domain.PackItem

class WeightPackItemSort(private val ascending: Boolean = true) : IPackItemSort {
    override fun sort(items: List<PackItem>): List<PackItem> {
        return items.sortedWith(
            compareBy(
                {
                    val weight = it.packedWeight?.convertTo(WeightUnits.Grams)?.weight
                    when {
                        weight == null -> Float.POSITIVE_INFINITY
                        ascending -> weight
                        else -> -weight
                    }
                },
                { it.category.name },
                { it.name },
                { it.id }
            )
        )
    }
}