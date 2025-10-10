package com.kylecorry.trail_sense.tools.navigation.infrastructure.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationBearing
import java.time.Instant

@Entity(tableName = "navigation_bearings")
data class NavigationBearingEntity(
    @ColumnInfo(name = "bearing") val bearing: Float,
    @ColumnInfo(name = "start_latitude") val startLatitude: Double?,
    @ColumnInfo(name = "start_longitude") val startLongitude: Double?,
    @ColumnInfo(name = "start_time") val startTime: Long?,
    @ColumnInfo(name = "is_active") val isActive: Boolean
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0

    val startLocation: Coordinate?
        get() = startLatitude?.let {
            startLongitude?.let {
                Coordinate(
                    startLatitude,
                    startLongitude
                )
            }
        }

    fun toNavigationBearing(): NavigationBearing {
        return NavigationBearing(
            id,
            bearing,
            startLocation,
            startTime?.let { Instant.ofEpochMilli(it) },
            isActive
        )
    }

    companion object {
        fun from(bearing: NavigationBearing): NavigationBearingEntity {
            return NavigationBearingEntity(
                bearing.bearing,
                bearing.startLocation?.latitude,
                bearing.startLocation?.longitude,
                bearing.startTime?.toEpochMilli(),
                bearing.isActive
            ).apply {
                id = bearing.id
            }
        }
    }
}
