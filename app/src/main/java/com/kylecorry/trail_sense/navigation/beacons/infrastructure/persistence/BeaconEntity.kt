package com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.shared.colors.AppColor

@Entity(
    tableName = "beacons"
)
data class BeaconEntity(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "visible") val visible: Boolean,
    @ColumnInfo(name = "comment") val comment: String?,
    @ColumnInfo(name = "beacon_group_id") val beaconGroupId: Long?,
    @ColumnInfo(name = "elevation") val elevation: Float?,
    @ColumnInfo(name = "temporary") val temporary: Boolean,
    @ColumnInfo(name = "owner") val owner: BeaconOwner,
    @ColumnInfo(name = "color") val color: AppColor
) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0

    val coordinate: Coordinate
        get() = Coordinate(latitude, longitude)

    fun toBeacon(): Beacon {
        return Beacon(
            id,
            name,
            coordinate,
            visible,
            comment,
            beaconGroupId,
            elevation,
            temporary = temporary,
            color = color.color,
            owner = owner
        )
    }


    companion object {
        fun from(beacon: Beacon): BeaconEntity {
            return BeaconEntity(
                beacon.name,
                beacon.coordinate.latitude,
                beacon.coordinate.longitude,
                beacon.visible,
                beacon.comment,
                beacon.parentId,
                beacon.elevation,
                beacon.temporary,
                beacon.owner,
                AppColor.values().firstOrNull { it.color == beacon.color } ?: AppColor.Orange
            ).also {
                it.id = beacon.id
            }
        }
    }

}