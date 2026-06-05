package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.calibration

import android.graphics.Bitmap
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PixelBounds
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.calibration.corners.MapCornerDetector
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.calibration.rotation.MapRotationCalculator

class PhotoMapCalibrator {

    private val rotationCalculator = MapRotationCalculator()
    private val cornerDetector = MapCornerDetector()

    fun calculateRotation(map: PhotoMap): Float {
        return rotationCalculator.calculate(map)
    }

    fun detectCorners(bitmap: Bitmap): PixelBounds? {
        return cornerDetector.detect(bitmap)
    }

}
