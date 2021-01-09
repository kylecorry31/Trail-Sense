package com.kylecorry.trail_sense.tools.backtrack.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import java.time.Instant

@Entity(tableName = "waypoints")
data class WaypointEntity(
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "altitude") val altitude: Float?,
    @ColumnInfo(name = "createdOn") val createdOn: Long

) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0

    val createdInstant: Instant
        get() = Instant.ofEpochMilli(createdOn)

    val coordinate: Coordinate
        get() = Coordinate(latitude, longitude)

}