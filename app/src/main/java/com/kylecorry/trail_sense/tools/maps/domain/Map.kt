package com.kylecorry.trail_sense.tools.maps.domain

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.projections.IMapProjection
import com.kylecorry.sol.units.Distance

data class Map(
    val id: Long,
    val name: String,
    val filename: String,
    val calibrationPoints: List<MapCalibrationPoint>,
    val warped: Boolean,
    val rotated: Boolean,
    // TODO: Support projections other than mercator
    val projection: MapProjectionType = MapProjectionType.Equirectangular
) {

    fun projection(width: Float, height: Float): IMapProjection {
        return CalibratedProjection(calibrationPoints.map {
            it.imageLocation.toPixels(width, height) to it.location
        }) { bounds, size -> MapProjectionFactory(bounds, size).getProjection(projection) }
    }

    fun distancePerPixel(width: Float, height: Float): Distance? {
        if (calibrationPoints.size < 2) {
            // Or throw, not enough calibration points
            return null
        }

        val first = calibrationPoints[0]
        val second = calibrationPoints[1]
        val firstPixels = first.imageLocation.toPixels(width, height)
        val secondPixels = second.imageLocation.toPixels(width, height)

        val meters = first.location.distanceTo(second.location)
        val pixels = firstPixels.distanceTo(secondPixels)

        if (meters == 0f || pixels == 0f) {
            // Or throw, calibration points are the same
            return null
        }

        return Distance.meters(meters / pixels)
    }

    fun boundary(width: Float, height: Float): CoordinateBounds? {
        if (calibrationPoints.isEmpty()) {
            // Or throw, not enough calibration points
            return null
        }

        val projection = projection(width, height)

        val topLeft = projection.toCoordinate(Vector2(0f, 0f))
        val bottomLeft = projection.toCoordinate(Vector2(0f, height))
        val topRight = projection.toCoordinate(Vector2(width, 0f))
        val bottomRight = projection.toCoordinate(Vector2(width, height))

        return CoordinateBounds.from(listOf(topLeft, bottomLeft, topRight, bottomRight))
    }

}
