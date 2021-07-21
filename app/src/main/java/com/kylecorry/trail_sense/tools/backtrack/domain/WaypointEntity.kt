package com.kylecorry.trail_sense.tools.backtrack.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.persistence.WaypointRepo
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.geo.PathPoint
import com.kylecorry.trailsensecore.domain.network.CellNetwork
import com.kylecorry.trailsensecore.domain.network.CellNetworkQuality
import com.kylecorry.trailsensecore.domain.units.Quality
import java.time.Instant

@Entity(tableName = "waypoints")
data class WaypointEntity(
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "altitude") val altitude: Float?,
    @ColumnInfo(name = "createdOn") val createdOn: Long,
    @ColumnInfo(name = "cellType") val cellTypeId: Int?,
    @ColumnInfo(name = "cellQuality") val cellQualityId: Int?

) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0

    val createdInstant: Instant
        get() = Instant.ofEpochMilli(createdOn)

    val coordinate: Coordinate
        get() = Coordinate(latitude, longitude)

    val cellQuality: Quality
        get() {
            return Quality.values().firstOrNull { it.ordinal == cellQualityId } ?: Quality.Unknown
        }

    val cellNetwork: CellNetwork?
        get() {
            return CellNetwork.values().firstOrNull { it.id == cellTypeId }
        }

    fun toPathPoint(): PathPoint {
        val network = if (cellNetwork == null) null else CellNetworkQuality(cellNetwork!!, cellQuality)
        return PathPoint(id, WaypointRepo.BACKTRACK_PATH_ID, coordinate, time = createdInstant, cellSignal = network)
    }

}