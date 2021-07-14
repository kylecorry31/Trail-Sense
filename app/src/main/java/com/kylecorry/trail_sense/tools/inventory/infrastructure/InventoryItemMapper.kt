package com.kylecorry.trail_sense.tools.inventory.infrastructure

import com.kylecorry.trail_sense.tools.inventory.domain.InventoryItem
import com.kylecorry.trail_sense.tools.inventory.domain.PackItem

// TODO: Add all the fields to an inventory item
// TODO: Use this at the repo layer
class InventoryItemMapper {

    fun mapToPackItem(item: InventoryItem): PackItem {
        return PackItem(item.id, 0, item.name, item.category, item.amount)
    }

    fun mapToInventoryItem(item: PackItem): InventoryItem {
        return InventoryItem(item.name, item.category, item.amount).also {
            it.id = item.id
        }
    }

}