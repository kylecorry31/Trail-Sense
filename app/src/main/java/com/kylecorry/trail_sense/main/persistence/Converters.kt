package com.kylecorry.trail_sense.main.persistence

import androidx.room.TypeConverter
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.WeightUnits
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.withId
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconIcon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.photo_maps.domain.MapProjectionType
import com.kylecorry.trail_sense.tools.packs.domain.ItemCategory
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle
import com.kylecorry.trail_sense.tools.paths.domain.PathPointColoringStyle
import java.time.Duration
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
    fun fromCloudGenus(value: CloudGenus?): Int? {
        // This was previously the ordinal (zero-indexed), which is why I need to subtract 1
        return value?.id?.toInt()?.minus(1)
    }

    @TypeConverter
    fun toCloudGenus(value: Int?): CloudGenus? {
        // This was previously the ordinal (zero-indexed), which is why I need to add 1
        return CloudGenus.entries.firstOrNull { it.id == value?.plus(1)?.toLong() }
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
    fun fromDuration(value: Duration): Long {
        return value.toMillis()
    }

    @TypeConverter
    fun toDuration(value: Long): Duration {
        return Duration.ofMillis(value)
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
        return AppColor.values().withId(value) ?: AppColor.Orange
    }

    @TypeConverter
    fun fromBeaconIcon(value: BeaconIcon?): Long? {
        return value?.id
    }

    @TypeConverter
    fun toBeaconIcon(value: Long?): BeaconIcon? {
        value ?: return null
        return BeaconIcon.values().withId(value)
    }

    @TypeConverter
    fun fromPathPointColoringStyle(value: PathPointColoringStyle): Long {
        return value.id
    }

    @TypeConverter
    fun toPathPointColoringStyle(value: Long): PathPointColoringStyle {
        return PathPointColoringStyle.values().withId(value) ?: PathPointColoringStyle.None
    }

    @TypeConverter
    fun fromLineStyle(value: LineStyle): Int {
        return value.id
    }

    @TypeConverter
    fun toLineStyle(value: Int): LineStyle {
        return LineStyle.values().firstOrNull { it.id == value } ?: LineStyle.Dotted
    }

    @TypeConverter
    fun fromMapProjectionType(mapProjectionType: MapProjectionType): Long {
        return mapProjectionType.id
    }

    @TypeConverter
    fun toMapProjectionType(value: Long): MapProjectionType {
        return MapProjectionType.values().withId(value) ?: MapProjectionType.Mercator
    }
}