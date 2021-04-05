package com.kylecorry.trail_sense.tools.maps.domain

import com.kylecorry.trailsensecore.domain.geo.Coordinate

data class MapEntity(
    val name: String,
    val filename: String,
    val latitude1: Double?,
    val longitude1: Double?,
    val percentX1: Float?,
    val percentY1: Float?,
    val latitude2: Double?,
    val longitude2: Double?,
    val percentX2: Float?,
    val percentY2: Float?
) {
    var id: Long = 0

    fun toMap(): Map {
        val points = mutableListOf<MapCalibrationPoint>()

        if (percentX1 != null && percentY1 != null && longitude1 != null && latitude1 != null){
            points.add(MapCalibrationPoint(Coordinate(latitude1, longitude1), PercentCoordinate(percentX1, percentY1)))
        }

        if (percentX2 != null && percentY2 != null && longitude2 != null && latitude2 != null){
            points.add(MapCalibrationPoint(Coordinate(latitude2, longitude2), PercentCoordinate(percentX2, percentY2)))
        }

        return Map(id, name, filename, points)
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
            ).also {
                it.id = map.id
            }
        }

        fun new(name: String, filename: String): MapEntity {
            return MapEntity(name, filename, null, null, null, null, null, null, null, null)
        }
    }

}
