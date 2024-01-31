package com.kylecorry.trail_sense.tools.augmented_reality.domain

import com.kylecorry.andromeda.core.bitmap.BitmapUtils.resizeToFit
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Quaternion
import com.kylecorry.trail_sense.shared.camera.GrayscaleMomentFinder
import com.kylecorry.trail_sense.shared.views.CameraView
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.AugmentedRealityCoordinate
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView

class SunCalibrator {

    private val astro = AstronomyService()

    suspend fun calibrate(view: AugmentedRealityView, camera: CameraView): Quaternion? {
        // TODO: Get current orientation
        val image = camera.previewImage ?: return null
        return onDefault {
            // Scale the image to fit in 100x100
            val scaled = image.resizeToFit(100, 100)
            val scaledWidth = scaled.width
            val scaledHeight = scaled.height
            if (scaled != image) {
                image.recycle()
            }

            val momentFinder = GrayscaleMomentFinder(240, 5)

            try {
                val moment = momentFinder.getMoment(scaled) ?: return@onDefault null

                // Scale to viewport
                val xPct = moment.x / scaledWidth
                val yPct = moment.y / scaledHeight
                val actualPixel = PixelCoordinate(xPct * view.width, yPct * view.height)
                // TODO: Convert the pixel into a horizon coordinate

                val predictedLocation = AugmentedRealityCoordinate.fromSpherical(
                    astro.getSunAzimuth(view.location).value,
                    astro.getSunAltitude(view.location),
                    Float.MAX_VALUE,
                    true
                )
                // TODO: It may be easier to make the prediction location relative to the current orientation and compare the two

                val delta = Quaternion.zero

                // TODO: Calculate the quaternion needed to rotate the predicted position to the actual position
                delta
            } finally {
                scaled.recycle()
            }
        }
    }

}