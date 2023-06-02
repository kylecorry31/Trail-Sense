package com.kylecorry.trail_sense.tools.maps.domain

import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.roundNearestAngle
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.projections.IMapProjection
import com.kylecorry.sol.units.Distance

// Projection: Onto base rotation image
// Distance per pixel: On exact rotation image
// Calibration points: On exact rotation image
// Boundary: On exact rotation image


data class PhotoMap(
    override val id: Long,
    override val name: String,
    val filename: String,
    val calibration: MapCalibration,
    val metadata: MapMetadata,
    override val parentId: Long? = null
) : IMap {
    override val isGroup = false
    override val count: Int? = null

    private var calculatedBounds: CoordinateBounds? = null
    private val rotationService = PhotoMapRotationService(this)

    val isCalibrated: Boolean
        get() = calibration.calibrationPoints.size >= 2 && metadata.size.width > 0 && metadata.size.height > 0

    private fun getRotatedPoints(): List<MapCalibrationPoint> {
        return rotationService.getCalibrationPoints()
    }

    /**
     * The projection onto the image (with base rotation applied, ex. 0, 90, 180, 270)
     */
    fun projection(): IMapProjection {
        val calibratedSize = calibratedSize()
        return projection(calibratedSize.width, calibratedSize.height)
    }

    private fun projection(width: Float, height: Float): IMapProjection {
        val calibrationPoints = getRotatedPoints()
        val projection = CalibratedProjection(calibrationPoints.map {
            it.imageLocation.toPixels(width, height) to it.location
        }, MapProjectionFactory().getProjection(metadata.projection))
        val size = baseSize()
        return RotatedProjection(
            projection,
            size,
            SolMath.deltaAngle(baseRotation().toFloat(), calibration.rotation)
        )
    }

    fun distancePerPixel(): Distance? {
        val calibratedSize = calibratedSize()
        return distancePerPixel(calibratedSize.width, calibratedSize.height)
    }

    private fun distancePerPixel(width: Float, height: Float): Distance? {
        if (!isCalibrated) {
            // Or throw, not enough calibration points
            return null
        }

        val calibrationPoints = getRotatedPoints()

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

    fun boundary(): CoordinateBounds? {
        if (calculatedBounds != null) {
            return calculatedBounds
        }
        val size = calibratedSize()
        calculatedBounds = boundary(size.width, size.height)
        return calculatedBounds
    }

    fun baseRotation(): Int {
        return calibration.rotation.roundNearestAngle(90f).toInt()
    }

    /**
     * The size of the image with exact rotation applied
     */
    fun calibratedSize(): Size {
        return metadata.size.rotate(calibration.rotation)
    }

    /**
     * The size of the image with base rotation applied (ex. 0, 90, 180, 270)
     */
    fun baseSize(): Size {
        return metadata.size.rotate(baseRotation().toFloat())
    }

    fun boundary(width: Float, height: Float): CoordinateBounds? {
        if (!isCalibrated) {
            return null
        }

        // TODO: This projection needs to stay unrotated (maybe?)
        val projection = projection(width, height)

        val topLeft = projection.toCoordinate(Vector2(0f, 0f))
        val bottomLeft = projection.toCoordinate(Vector2(0f, height))
        val topRight = projection.toCoordinate(Vector2(width, 0f))
        val bottomRight = projection.toCoordinate(Vector2(width, height))

        return CoordinateBounds.from(listOf(topLeft, bottomLeft, topRight, bottomRight))
    }

}
