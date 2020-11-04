package com.kylecorry.trail_sense.tools.inventory.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "items"
)
data class InventoryItem(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "category") val category: ItemCategory,
    @ColumnInfo(name = "amount") val amount: Double = 0.0
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0
}