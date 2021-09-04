package com.kylecorry.trail_sense.tools.maps.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kylecorry.andromeda.core.units.Coordinate

@Entity(tableName = "maps")
data class MapEntity(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "filename") val filename: String,
    @ColumnInfo(name = "latitude1") val latitude1: Double?,
    @ColumnInfo(name = "longitude1") val longitude1: Double?,
    @ColumnInfo(name = "percentX1") val percentX1: Float?,
    @ColumnInfo(name = "percentY1") val percentY1: Float?,
    @ColumnInfo(name = "latitude2") val latitude2: Double?,
    @ColumnInfo(name = "longitude2") val longitude2: Double?,
    @ColumnInfo(name = "percentX2") val percentX2: Float?,
    @ColumnInfo(name = "percentY2") val percentY2: Float?,
    @ColumnInfo(name = "warped") val warped: Boolean,
    @ColumnInfo(name = "rotated") val rotated: Boolean
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0

    fun toMap(): Map {
        val points = mutableListOf<MapCalibrationPoint>()

        if (percentX1 != null && percentY1 != null && longitude1 != null && latitude1 != null) {
            points.add(
                MapCalibrationPoint(
                    Coordinate(latitude1, longitude1),
                    PercentCoordinate(percentX1, percentY1)
                )
            )
        }

        if (percentX2 != null && percentY2 != null && longitude2 != null && latitude2 != null) {
            points.add(
                MapCalibrationPoint(
                    Coordinate(latitude2, longitude2),
                    PercentCoordinate(percentX2, percentY2)
                )
            )
        }

        return Map(id, name, filename, points, warped, rotated)
    }

    companion object {
        fun from(map: Map): MapEntity {
            return MapEntity(
                map.name,
                map.filename,
                if (map.calibrationPoints.isNotEmpty()) map.calibrationPoints[0].location.latitude else null,
                if (map.calibrationPoints.isNotEmpty()) map.calibrationPoints[0].location.longitude else null,
                if (map.calibrationPoints.isNotEmpty()) map.calibrationPoints[0].imageLocation.x else null,
                if (map.calibrationPoints.isNotEmpty()) map.calibrationPoints[0].imageLocation.y else null,
                if (map.calibrationPoints.size > 1) map.calibrationPoints[1].location.latitude else null,
                if (map.calibrationPoints.size > 1) map.calibrationPoints[1].location.longitude else null,
                if (map.calibrationPoints.size > 1) map.calibrationPoints[1].imageLocation.x else null,
                if (map.calibrationPoints.size > 1) map.calibrationPoints[1].imageLocation.y else null,
                map.warped,
                map.rotated
            ).also {
                it.id = map.id
            }
        }

        fun new(name: String, filename: String): MapEntity {
            return MapEntity(
                name, filename, null, null, null, null, null, null, null, null,
                warped = false,
                rotated = false
            )
        }
    }

}
