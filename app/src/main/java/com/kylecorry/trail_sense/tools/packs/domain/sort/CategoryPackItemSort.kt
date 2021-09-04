package com.kylecorry.trail_sense.tools.packs.domain.sort

import com.kylecorry.trail_sense.tools.packs.domain.PackItem

class CategoryPackItemSort : IPackItemSort {
    override fun sort(items: List<PackItem>): List<PackItem> {
        return items.sortedWith(
            compareBy(
                { it.category.name },
                { it.name },
                { it.id }
            )
        )
    }
}