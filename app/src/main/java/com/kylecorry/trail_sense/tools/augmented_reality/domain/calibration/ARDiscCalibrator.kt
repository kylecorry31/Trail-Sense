package com.kylecorry.trail_sense.tools.augmented_reality.domain.calibration

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.sol.math.trigonometry.Trigonometry
import com.kylecorry.trail_sense.shared.views.CameraView
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.AugmentedRealityCoordinate
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView

/**
 * Calibrates from the sun or moon anywhere in the camera frame (it doesn't need to be centered).
 * The detected disc is mapped to a direction with the AR view's own camera mapping, so after
 * calibration the drawn body lines up with the real one.
 */
class ARDiscCalibrator(
    private val reference: AugmentedRealityCoordinate,
    private val finder: BrightDiscFinder,
    private val maxCompassErrorDegrees: Float = 20f,
    private val maxDiscDegrees: Float = 4f
) : IARCalibrator {

    data class Result(
        val offset: Float,
        val detectedPixel: PixelCoordinate,
        val predictedPixel: PixelCoordinate,
        val altitudeError: Float,
        val radiusPixels: Double
    )

    var lastResult: Result? = null
        private set

    override suspend fun calibrateBearing(view: AugmentedRealityView, camera: CameraView): Float? {
        val bitmap = onMain { camera.previewImage } ?: return null
        val predicted = onMain { view.toPixel(reference, nanIfBehindCamera = true) }
        if (!predicted.x.isFinite() || !predicted.y.isFinite()) {
            bitmap.recycle()
            return null
        }
        val searchRadius = onMain { view.sizeToPixel(maxCompassErrorDegrees * 2) / 2 }
        val maxRadius = onMain { view.sizeToPixel(maxDiscDegrees) / 2 }

        val detection = onDefault {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            // The preview bitmap matches the view size, but scale just in case
            val scaleX = bitmap.width / view.width.toDouble()
            val scaleY = bitmap.height / view.height.toDouble()
            finder.find(
                pixels,
                bitmap.width,
                bitmap.height,
                predicted.x * scaleX,
                predicted.y * scaleY,
                searchRadius * scaleX,
                maxRadius * scaleX
            )?.let { it.copy(x = it.x / scaleX, y = it.y / scaleY, radius = it.radius / scaleX) }
        }
        bitmap.recycle()
        detection ?: return null

        val detectedPixel = PixelCoordinate(detection.x.toFloat(), detection.y.toFloat())
        val (detected, actual) = onMain {
            view.toCoordinate(detectedPixel) to AugmentedRealityCoordinate(
                view.getActualPoint(reference.position, reference.isTrueNorth),
                view.isTrueNorth
            )
        }
        val offset = Trigonometry.deltaAngle(detected.bearing, actual.bearing)
        lastResult = Result(
            offset,
            detectedPixel,
            predicted,
            actual.elevation - detected.elevation,
            detection.radius
        )
        return offset
    }
}
