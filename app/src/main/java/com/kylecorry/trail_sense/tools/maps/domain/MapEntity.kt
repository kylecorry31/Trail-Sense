package com.kylecorry.trail_sense.tools.maps.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.units.Coordinate

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
    @ColumnInfo(name = "rotated") val rotated: Boolean,
    @ColumnInfo(name = "projection") val projection: MapProjectionType = MapProjectionType.Mercator,
    @ColumnInfo(name = "rotation") val rotation: Int = 0,
    @ColumnInfo(name = "parent") val parent: Long? = null
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

        val calibration = MapCalibration(warped, rotated, rotation, points)

        // This gets populated by the repo
        val metadata = MapMetadata(
            Size(0f, 0f),
            0,
            projection = projection
        )

        return Map(id, name, filename, calibration, metadata, parent)
    }

    companion object {
        fun from(map: Map): MapEntity {
            val calibration = map.calibration
            return MapEntity(
                map.name,
                map.filename,
                if (calibration.calibrationPoints.isNotEmpty()) calibration.calibrationPoints[0].location.latitude else null,
                if (calibration.calibrationPoints.isNotEmpty()) calibration.calibrationPoints[0].location.longitude else null,
                if (calibration.calibrationPoints.isNotEmpty()) calibration.calibrationPoints[0].imageLocation.x else null,
                if (calibration.calibrationPoints.isNotEmpty()) calibration.calibrationPoints[0].imageLocation.y else null,
                if (calibration.calibrationPoints.size > 1) calibration.calibrationPoints[1].location.latitude else null,
                if (calibration.calibrationPoints.size > 1) calibration.calibrationPoints[1].location.longitude else null,
                if (calibration.calibrationPoints.size > 1) calibration.calibrationPoints[1].imageLocation.x else null,
                if (calibration.calibrationPoints.size > 1) calibration.calibrationPoints[1].imageLocation.y else null,
                calibration.warped,
                calibration.rotated,
                map.metadata.projection,
                calibration.rotation,
                map.parentId
            ).also {
                it.id = map.id
            }
        }
    }

}
