package com.kylecorry.trail_sense.shared

import androidx.room.TypeConverter
import com.kylecorry.andromeda.core.units.WeightUnits
import com.kylecorry.trailsensecore.domain.navigation.BeaconOwner
import com.kylecorry.trailsensecore.domain.packs.ItemCategory
import java.time.Instant

class Converters {
    @TypeConverter
    fun fromItemCategory(value: ItemCategory): Int {
        return value.id
    }

    @TypeConverter
    fun toItemCategory(value: Int): ItemCategory {
        return ItemCategory.values().first { it.id == value }
    }

    @TypeConverter
    fun fromWeightUnit(value: WeightUnits?): Int? {
        return value?.id
    }

    @TypeConverter
    fun toWeightUnit(value: Int?): WeightUnits? {
        return WeightUnits.values().firstOrNull { it.id == value }
    }

    @TypeConverter
    fun fromInstant(value: Instant): Long {
        return value.toEpochMilli()
    }

    @TypeConverter
    fun toInstant(value: Long): Instant {
        return Instant.ofEpochMilli(value)
    }

    @TypeConverter
    fun fromBeaconOwner(value: BeaconOwner): Int {
        return value.id
    }

    @TypeConverter
    fun toBeaconOwner(value: Int): BeaconOwner {
        return BeaconOwner.values().firstOrNull { it.id == value } ?: BeaconOwner.User
    }

    @TypeConverter
    fun fromAppColor(value: AppColor): Int {
        return value.id
    }

    @TypeConverter
    fun toAppColor(value: Int): AppColor {
        return AppColor.values().firstOrNull { it.id == value } ?: AppColor.Orange
    }
}