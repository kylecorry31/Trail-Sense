package com.kylecorry.trail_sense.tools.maps.domain

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Distance

data class Map(
    val id: Long,
    val name: String,
    val filename: String,
    val calibrationPoints: List<MapCalibrationPoint>,
    val warped: Boolean,
    val rotated: Boolean
) {

    fun projection(width: Float, height: Float): IProjection {
        // TODO: Support projections other than mercator
        return CalibratedMercatorProjection(calibrationPoints.map {
            it.imageLocation.toPixels(width, height) to it.location
        })
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

        val topLeft = projection.toCoordinate(PixelCoordinate(0f, 0f)) ?: return null
        val bottomLeft = projection.toCoordinate(PixelCoordinate(0f, height)) ?: return null
        val topRight = projection.toCoordinate(PixelCoordinate(width, 0f)) ?: return null
        val bottomRight = projection.toCoordinate(PixelCoordinate(width, height)) ?: return null

        return CoordinateBounds.from(listOf(topLeft, bottomLeft, topRight, bottomRight))
    }

}
