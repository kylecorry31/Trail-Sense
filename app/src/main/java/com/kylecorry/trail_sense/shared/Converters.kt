package com.kylecorry.trail_sense.shared

import androidx.room.TypeConverter
import com.kylecorry.trail_sense.tools.inventory.domain.ItemCategory

class Converters {
    @TypeConverter
    fun fromItemCategory(value: ItemCategory): Int{
        return value.id
    }

    @TypeConverter
    fun toItemCategory(value: Int): ItemCategory {
        return ItemCategory.values().first { it.id == value }
    }
}