package com.kylecorry.trail_sense.tools.maps.domain.projections

import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.science.geology.projections.IMapProjection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.maps.domain.MapProjectionFactory
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMapRotationService

class PhotoMapProjection(private val map: PhotoMap) : IMapProjection {

    private val rotationService = PhotoMapRotationService(map)
    private val projection by lazy { calculateProjection() }

    override fun toCoordinate(pixel: Vector2): Coordinate {
        return projection.toCoordinate(pixel)
    }

    override fun toPixels(location: Coordinate): Vector2 {
        return projection.toPixels(location)
    }

    private fun calculateProjection(): IMapProjection {
        val rotatedSize = map.calibratedSize()
        val calibrationPoints = rotationService.getCalibrationPoints()
        val projection = CalibratedProjection(calibrationPoints.map {
            it.imageLocation.toPixels(rotatedSize.width, rotatedSize.height) to it.location
        }, MapProjectionFactory().getProjection(map.metadata.projection))

        // No need to rotate it back to the base rotation if it's already at the base rotation
        if (map.calibration.rotation % 90f == 0f) {
            return projection
        }

        val size = map.baseSize()
        val baseRotation = map.baseRotation()
        return RotatedProjection(
            projection,
            size,
            SolMath.deltaAngle(baseRotation.toFloat(), map.calibration.rotation)
        )
    }

}