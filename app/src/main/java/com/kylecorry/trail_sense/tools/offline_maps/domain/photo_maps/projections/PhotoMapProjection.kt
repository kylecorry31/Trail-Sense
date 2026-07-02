package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.projections

import com.kylecorry.andromeda.core.units.PercentCoordinate
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.arithmetic.Arithmetic
import com.kylecorry.sol.math.trigonometry.Trigonometry
import com.kylecorry.sol.science.geography.projections.IMapProjection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.rotateInRect
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.calibration.MapCalibrationPoint
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap

class PhotoMapProjection(
    private val map: PhotoMap,
    private val usePdf: Boolean = true,
    private val useBaseRotation: Boolean = false
) :
    IMapProjection {
    private val projection by lazy { calculateProjection() }

    override fun toCoordinate(pixel: Vector2): Coordinate {
        return projection.toCoordinate(pixel)
    }

    override fun toPixels(location: Coordinate): Vector2 {
        return projection.toPixels(location)
    }

    override fun toPixels(
        latitude: Double,
        longitude: Double
    ): Vector2 {
        return projection.toPixels(latitude, longitude)
    }

    private fun calculateProjection(): IMapProjection {
        val rotatedSize = map.calibratedSize(usePdf)
        val calibrationPoints = getCalibrationPoints()
        val projection = CalibratedProjection(calibrationPoints.map {
            it.imageLocation.toPixels(rotatedSize.width, rotatedSize.height) to it.location
        }, MapProjectionFactory().getProjection(map.georeference.projectionType))

        val size = if (useBaseRotation) {
            map.baseSize(usePdf)
        } else {
            map.unrotatedSize(usePdf)
        }
        val baseRotation = if (useBaseRotation) {
            map.baseRotation()
        } else {
            0f
        }

        val angle = Trigonometry.deltaAngle(
            baseRotation.toFloat(),
            map.georeference.rotation
        )

        if (Arithmetic.isZero(angle)) {
            return projection
        }

        return RotatedProjection(
            projection,
            size,
            angle
        )
    }

    private fun getCalibrationPoints(): List<MapCalibrationPoint> {
        val newSize = map.calibratedSize()
        return map.georeference.calibrationPoints.map {
            // Convert to pixels
            val pixel = it.imageLocation.toPixels(map.georeference.size.width, map.georeference.size.height)
            // Rotate it around the center of the image
            val rotated = pixel.rotateInRect(map.georeference.rotation, map.georeference.size)
            // Convert back to percent
            val percent = PercentCoordinate(rotated.x / newSize.width, rotated.y / newSize.height)
            MapCalibrationPoint(it.location, percent)
        }
    }

}
