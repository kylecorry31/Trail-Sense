package com.kylecorry.trail_sense.tools.maps.domain

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.projections.IMapProjection
import com.kylecorry.sol.units.Distance

data class PhotoMap(
    override val id: Long,
    override val name: String,
    val filename: String,
    val calibration: MapCalibration,
    val metadata: MapMetadata,
    override val parentId: Long? = null
): IMap {
    override val isGroup = false
    override val count: Int? = null
    
    private var calculatedBounds: CoordinateBounds? = null

    val isCalibrated: Boolean
        get() = calibration.calibrationPoints.size >= 2 && metadata.size.width > 0 && metadata.size.height > 0

    fun projection(width: Float, height: Float): IMapProjection {
        return CalibratedProjection(calibration.calibrationPoints.map {
            it.imageLocation.toPixels(width, height) to it.location
        }, MapProjectionFactory().getProjection(metadata.projection))
    }

    fun distancePerPixel(width: Float, height: Float): Distance? {
        if (!isCalibrated) {
            // Or throw, not enough calibration points
            return null
        }

        val first = calibration.calibrationPoints[0]
        val second = calibration.calibrationPoints[1]
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

    fun boundary(): CoordinateBounds? {
        if (calculatedBounds != null) {
            return calculatedBounds
        }
        val size = metadata.size
        val width = if (calibration.rotation == 90 || calibration.rotation == 270) {
            size.height
        } else {
            size.width
        }
        val height = if (calibration.rotation == 90 || calibration.rotation == 270) {
            size.width
        } else {
            size.height
        }
        calculatedBounds = boundary(width, height)
        return calculatedBounds
    }

    fun boundary(width: Float, height: Float): CoordinateBounds? {
        if (!isCalibrated) {
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
