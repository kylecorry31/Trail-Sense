package com.kylecorry.trail_sense.tools.packs.domain.sort

import com.kylecorry.trail_sense.tools.packs.domain.PackItem

class PackedPercentPackItemSort(private val ascending: Boolean = true) : IPackItemSort {
    override fun sort(items: List<PackItem>): List<PackItem> {
        return items.sortedWith(
            compareBy(
                { if (ascending) it.percentPacked else -it.percentPacked },
                { it.category.name },
                { it.name },
                { it.id }
            )
        )
    }
}