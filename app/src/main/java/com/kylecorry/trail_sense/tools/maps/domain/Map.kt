package com.kylecorry.trail_sense.tools.maps.domain

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance

data class Map(
    val id: Long,
    val name: String,
    val filename: String,
    val calibrationPoints: List<MapCalibrationPoint>,
    val warped: Boolean,
    val rotated: Boolean
) {

    fun getCoordinate(pixels: PixelCoordinate, width: Float, height: Float): Coordinate? {
        val calibrator = CalibratedMapCoordinateConverter(calibrationPoints.map { it.imageLocation.toPixels(width, height) to it.location })
        return calibrator.toCoordinate(pixels)
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

        val first = calibrationPoints[0]
        val firstPixels = first.imageLocation.toPixels(width, height)
        val metersPerPixel = distancePerPixel(width, height)?.meters()?.distance ?: return null

        val north = first.location.plus(
            Distance.meters(firstPixels.y * metersPerPixel),
            Bearing.from(CompassDirection.North)
        ).latitude
        val south = first.location.plus(
            Distance.meters((height - firstPixels.y) * metersPerPixel),
            Bearing.from(CompassDirection.South)
        ).latitude
        val east = first.location.plus(
            Distance.meters((width - firstPixels.x) * metersPerPixel),
            Bearing.from(CompassDirection.East)
        ).longitude
        val west = first.location.plus(
            Distance.meters(firstPixels.x * metersPerPixel),
            Bearing.from(CompassDirection.West)
        ).longitude

        return CoordinateBounds(north, east, south, west)
    }

}
