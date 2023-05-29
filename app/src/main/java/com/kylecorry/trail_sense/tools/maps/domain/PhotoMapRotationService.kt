package com.kylecorry.trail_sense.tools.maps.domain

import com.kylecorry.sol.math.SolMath.cosDegrees
import com.kylecorry.sol.math.SolMath.sinDegrees
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.geometry.Size
import kotlin.math.abs

internal class PhotoMapRotationService(private val map: PhotoMap) {

    fun getSize(): Size {
        // TODO: Extract this to sol
        val sinAngle = sinDegrees(map.calibration.rotation.toFloat())
        val cosAngle = cosDegrees(map.calibration.rotation.toFloat())
        return Size(
            abs(map.metadata.size.width * cosAngle) + abs(map.metadata.size.height * sinAngle),
            abs(map.metadata.size.width * sinAngle) + abs(map.metadata.size.height * cosAngle),
        )
    }

    fun getCalibrationPoints(): List<MapCalibrationPoint> {
        val size = getSize()

        // Convert calibration points to pixels
        val pixels = map.calibration.calibrationPoints.map {
            it.imageLocation.toPixels(map.metadata.size.width, map.metadata.size.height)
        }

        // Rotate pixels around center
        val origin = Vector2(map.metadata.size.width / 2f, map.metadata.size.height / 2f)
        val newOrigin = Vector2(size.width / 2f, size.height / 2f)
        val rotated = pixels.map {
            Vector2(it.x - origin.x, it.y - origin.y)
                .rotate(map.calibration.rotation.toFloat())
                .plus(newOrigin)
        }

        // Convert rotated pixels to percent of new size
        val percent = rotated.map {
            PercentCoordinate(it.x / size.width, it.y / size.height)
        }

        // Remap percent to location
        return percent.mapIndexed { index, point ->
            MapCalibrationPoint(map.calibration.calibrationPoints[index].location, point)
        }
    }

}