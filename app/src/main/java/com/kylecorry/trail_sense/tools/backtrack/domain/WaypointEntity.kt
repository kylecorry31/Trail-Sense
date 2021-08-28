package com.kylecorry.trail_sense.tools.backtrack.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.units.Coordinate
import com.kylecorry.andromeda.signal.CellNetwork
import com.kylecorry.andromeda.signal.CellNetworkQuality
import com.kylecorry.trailsensecore.domain.geo.PathPoint
import java.time.Instant

@Entity(tableName = "waypoints")
data class WaypointEntity(
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "altitude") val altitude: Float?,
    @ColumnInfo(name = "createdOn") val createdOn: Long,
    @ColumnInfo(name = "cellType") val cellTypeId: Int?,
    @ColumnInfo(name = "cellQuality") val cellQualityId: Int?,
    @ColumnInfo(name = "pathId") val pathId: Long = 0

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
        val network =
            if (cellNetwork == null) null else CellNetworkQuality(cellNetwork!!, cellQuality)
        return PathPoint(
            id,
            pathId,
            coordinate,
            time = createdInstant,
            cellSignal = network,
            elevation = altitude
        )
    }

    companion object {
        fun from(point: PathPoint): WaypointEntity {
            return WaypointEntity(
                point.coordinate.latitude,
                point.coordinate.longitude,
                point.elevation,
                point.time?.toEpochMilli() ?: Instant.now().toEpochMilli(),
                point.cellSignal?.network?.id,
                point.cellSignal?.quality?.ordinal,
                point.pathId
            ).also {
                it.id = point.id
            }
        }
    }

}