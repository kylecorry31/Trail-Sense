package com.kylecorry.trail_sense.tools.packs.domain.sort

import com.kylecorry.trail_sense.tools.packs.domain.ItemCategory
import com.kylecorry.trail_sense.tools.packs.domain.PackItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class PackedPercentPackItemSortTest {

    @Test
    fun sortAsc() {
        val items = listOf(
            item(0, "Test", ItemCategory.Natural, 0.0, 1.0),
            item(1, "Test 1", ItemCategory.Other, 1.0, 2.0),
            item(2, "Test 2", ItemCategory.Other, 3.0, 4.0),
            item(3, "Something", ItemCategory.Electronics, 0.0, 1.0),
            item(4, "Test 1", ItemCategory.Other, 1.0, 1.0),
            item(5, "Test 1", ItemCategory.Other, 1.0, 0.0),
            item(6, "Test", ItemCategory.Other, 3.0, 1.0),
        )

        val sort = PackedPercentPackItemSort(true)
        val expected = listOf<Long>(3, 0, 1, 2, 4, 5, 6)

        val sorted = sort.sort(items).map { it.id }

        assertEquals(expected, sorted)
    }

    @Test
    fun sortDesc() {
        val items = listOf(
            item(0, "Test", ItemCategory.Natural, 0.0, 1.0),
            item(1, "Test 1", ItemCategory.Other, 1.0, 2.0),
            item(2, "Test 2", ItemCategory.Other, 3.0, 4.0),
            item(3, "Something", ItemCategory.Electronics, 0.0, 1.0),
            item(4, "Test 1", ItemCategory.Other, 1.0, 1.0),
            item(5, "Test 1", ItemCategory.Other, 1.0, 0.0),
            item(6, "Test", ItemCategory.Other, 3.0, 1.0),
        )

        val sort = PackedPercentPackItemSort(false)
        val expected = listOf<Long>(6, 4, 5, 2, 1, 3, 0)

        val sorted = sort.sort(items).map { it.id }

        assertEquals(expected, sorted)
    }

    private fun item(
        id: Long,
        name: String,
        category: ItemCategory,
        amount: Double,
        desired: Double = 0.0
    ): PackItem {
        return PackItem(id, 0, name, category, amount, desired)
    }
}