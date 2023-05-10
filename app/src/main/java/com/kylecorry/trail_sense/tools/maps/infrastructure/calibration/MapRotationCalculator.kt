package com.kylecorry.trail_sense.tools.maps.infrastructure.calibration

import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.trail_sense.shared.toVector2
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap
import kotlin.math.atan2

class MapRotationCalculator {

    fun calculate(map: PhotoMap): Int {
        val size = map.metadata.size // map.calibratedSize()
        val pixels = map.calibration.calibrationPoints.map {
            it.imageLocation.toPixels(
                size.width,
                size.height
            ).toVector2(size.height)
        }
        val locations = map.calibration.calibrationPoints.map { it.location }

        val pixelAngle = SolMath.normalizeAngle(
            -atan2(
                pixels[1].y - pixels[0].y,
                pixels[1].x - pixels[0].x
            ).toDegrees()
        ) + 90f

        val bearing = locations[0].bearingTo(locations[1])

        return SolMath.deltaAngle(pixelAngle, bearing.value).toInt()
    }

}