package com.kylecorry.trail_sense.tools.packs.domain.sort

import com.kylecorry.trail_sense.tools.packs.domain.ItemCategory
import com.kylecorry.trail_sense.tools.packs.domain.PackItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class CategoryPackItemSortTest {

    @Test
    fun sort() {
        val items = listOf(
            item(0, "Test", ItemCategory.Natural),
            item(1, "Test 1", ItemCategory.Other),
            item(2, "Test 2", ItemCategory.Other),
            item(3, "Something", ItemCategory.Electronics),
            item(4, "Test 1", ItemCategory.Other),
        )

        val sort = CategoryPackItemSort()
        val expected = listOf<Long>(3, 0, 1, 4, 2)

        val sorted = sort.sort(items).map { it.id }

        assertEquals(expected, sorted)
    }

    private fun item(id: Long, name: String, category: ItemCategory): PackItem {
        return PackItem(id, 0, name, category)
    }
}