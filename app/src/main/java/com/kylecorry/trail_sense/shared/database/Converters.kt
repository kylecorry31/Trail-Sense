package com.kylecorry.trail_sense.shared.database

import androidx.room.TypeConverter
import com.kylecorry.sol.units.WeightUnits
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.navigation.paths.domain.LineStyle
import com.kylecorry.trail_sense.navigation.paths.domain.PathPointColoringStyle
import com.kylecorry.trail_sense.tools.packs.domain.ItemCategory
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
    fun fromAppColor(value: AppColor): Long {
        return value.id
    }

    @TypeConverter
    fun toAppColor(value: Long): AppColor {
        return AppColor.values().firstOrNull { it.id == value } ?: AppColor.Orange
    }

    @TypeConverter
    fun fromPathPointColoringStyle(value: PathPointColoringStyle): Long {
        return value.id
    }

    @TypeConverter
    fun toPathPointColoringStyle(value: Long): PathPointColoringStyle {
        return PathPointColoringStyle.values().firstOrNull { it.id == value }
            ?: PathPointColoringStyle.None
    }

    @TypeConverter
    fun fromLineStyle(value: LineStyle): Int {
        return value.id
    }

    @TypeConverter
    fun toLineStyle(value: Int): LineStyle {
        return LineStyle.values().firstOrNull { it.id == value } ?: LineStyle.Dotted
    }
}