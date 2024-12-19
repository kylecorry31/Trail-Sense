package com.kylecorry.trail_sense.tools.augmented_reality.domain.calibration

import com.kylecorry.andromeda.bitmaps.BitmapUtils.resizeToFit
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.sol.math.SolMath.deltaAngle
import com.kylecorry.trail_sense.shared.camera.GrayscaleMomentFinder
import com.kylecorry.trail_sense.shared.views.CameraView
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView

class AutoSunCalibrator : IARCalibrator {

    private val astro = AstronomyService()

    override suspend fun calibrateBearing(view: AugmentedRealityView, camera: CameraView): Float? {
        // TODO: If the sun and actual are too far apart, return null
        val image = camera.previewImage ?: return null
        return onDefault {
            // Scale the image to fit in 100x100
            val scaled = image.resizeToFit(100, 100)
            val scaledWidth = scaled.width
            if (scaled != image) {
                image.recycle()
            }

            val momentFinder = GrayscaleMomentFinder(245f, 5)

            try {
                val moment = momentFinder.getMoment(scaled) ?: return@onDefault null

                // Scale to viewport
                val xPct = moment.x / scaledWidth

                // Determine the actual location of the sun
                // TODO: This doesn't factor in the inclination of the device or declination (this value should always be with declination applied)
                val actualBearing = (xPct - 0.5f) * camera.fov.first + view.azimuth

                // Get the predicted location of the sun
                val predictedBearing = astro.getSunAzimuth(view.location).value

                // Calculate the bearing difference
                deltaAngle(actualBearing, predictedBearing)
            } finally {
                scaled.recycle()
            }
        }
    }

}