package com.kylecorry.trail_sense.tools.photo_maps.domain.projections

import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.science.geography.projections.IMapProjection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.photo_maps.domain.MapProjectionFactory
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMapRotationService

class PhotoMapProjection(
    private val map: PhotoMap,
    private val usePdf: Boolean = true,
    private val useBaseRotation: Boolean = true
) :
    IMapProjection {

    private val rotationService = PhotoMapRotationService(map)
    private val projection by lazy { calculateProjection() }

    override fun toCoordinate(pixel: Vector2): Coordinate {
        return projection.toCoordinate(pixel)
    }

    override fun toPixels(location: Coordinate): Vector2 {
        return projection.toPixels(location)
    }

    private fun calculateProjection(): IMapProjection {
        val rotatedSize = map.calibratedSize(usePdf)
        val calibrationPoints = rotationService.getCalibrationPoints()
        val projection = CalibratedProjection(calibrationPoints.map {
            it.imageLocation.toPixels(rotatedSize.width, rotatedSize.height) to it.location
        }, MapProjectionFactory().getProjection(map.metadata.projection))

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

        val angle = SolMath.deltaAngle(
            baseRotation.toFloat(),
            map.calibration.rotation
        )

        if (SolMath.isZero(angle)) {
            return projection
        }

        return RotatedProjection(
            projection,
            size,
            angle
        )
    }

}