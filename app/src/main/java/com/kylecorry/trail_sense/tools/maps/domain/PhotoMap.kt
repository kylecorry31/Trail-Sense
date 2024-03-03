package com.kylecorry.trail_sense.tools.maps.domain

import com.kylecorry.sol.math.SolMath.roundNearestAngle
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geography.projections.IMapProjection
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.tools.maps.domain.projections.PhotoMapProjection
import com.kylecorry.trail_sense.tools.maps.domain.projections.distancePerPixel

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
    private val boundsLock = Any()

    private var cachedDistancePerPixel: Distance? = null
    private val distancePerPixelLock = Any()

    /**
     * The projection onto the image (with base rotation applied, ex. 0, 90, 180, 270)
     */
    val projection: IMapProjection by lazy { PhotoMapProjection(this) }

    /**
     * Determines if the map is calibrated
     */
    val isCalibrated: Boolean
        get() = calibration.calibrationPoints.size >= 2 && metadata.size.width > 0 && metadata.size.height > 0

    /**
     * The distance per pixel of the image
     * @return the distance per pixel or null if the map is not calibrated
     */
    fun distancePerPixel(): Distance? {

        if (!isCalibrated) {
            return null
        }

        synchronized(distancePerPixelLock) {
            if (cachedDistancePerPixel != null) {
                return cachedDistancePerPixel
            }

            cachedDistancePerPixel = projection.distancePerPixel(
                calibration.calibrationPoints[0].location,
                calibration.calibrationPoints[1].location
            )

            return cachedDistancePerPixel
        }
    }

    /**
     * The rotation of the image to the nearest 90 degrees
     */
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

    /**
     * The boundary of the image
     * @return the boundary or null if the map is not calibrated
     */
    fun boundary(): CoordinateBounds? {
        if (!isCalibrated) {
            return null
        }

        synchronized(boundsLock) {
            if (calculatedBounds != null) {
                return calculatedBounds
            }

            val size = baseSize()

            val topLeft = projection.toCoordinate(Vector2(0f, 0f))
            val bottomLeft = projection.toCoordinate(Vector2(0f, size.height))
            val topRight = projection.toCoordinate(Vector2(size.width, 0f))
            val bottomRight = projection.toCoordinate(Vector2(size.width, size.height))

            calculatedBounds =
                CoordinateBounds.from(listOf(topLeft, bottomLeft, topRight, bottomRight))
            return calculatedBounds
        }
    }

}
