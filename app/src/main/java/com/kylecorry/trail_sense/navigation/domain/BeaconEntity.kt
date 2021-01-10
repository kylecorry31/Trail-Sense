package com.kylecorry.trail_sense.navigation.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.navigation.Beacon

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
    @ColumnInfo(name = "elevation") val elevation: Float?
) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0

    val coordinate: Coordinate
        get() = Coordinate(latitude, longitude)

    fun toBeacon(): Beacon {
        return Beacon(id, name, coordinate, visible, comment, beaconGroupId, elevation)
    }


    companion object {
        fun from(beacon: Beacon): BeaconEntity {
            return BeaconEntity(beacon.name, beacon.coordinate.latitude, beacon.coordinate.longitude, beacon.visible, beacon.comment, beacon.beaconGroupId, beacon.elevation).also {
                it.id = beacon.id
            }
        }
    }

}