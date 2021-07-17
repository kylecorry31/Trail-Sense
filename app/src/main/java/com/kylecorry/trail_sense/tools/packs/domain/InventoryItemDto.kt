package com.kylecorry.trail_sense.tools.packs.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kylecorry.trailsensecore.domain.units.WeightUnits

@Entity(
    tableName = "items"
)
data class InventoryItemDto(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "packId") val packId: Long,
    @ColumnInfo(name = "category") val category: ItemCategory,
    @ColumnInfo(name = "amount") val amount: Double = 0.0,
    @ColumnInfo(name = "desiredAmount") val desiredAmount: Double = 0.0,
    @ColumnInfo(name = "weight") val weight: Float? = null,
    @ColumnInfo(name = "weightUnits") val weightUnits: WeightUnits? = null
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0
}