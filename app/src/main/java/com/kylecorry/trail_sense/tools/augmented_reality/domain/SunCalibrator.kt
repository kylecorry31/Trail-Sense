package com.kylecorry.trail_sense.tools.augmented_reality.domain

import com.kylecorry.andromeda.core.bitmap.BitmapUtils.resizeToFit
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Euler
import com.kylecorry.sol.math.Quaternion
import com.kylecorry.sol.math.QuaternionMath
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.roundPlaces
import com.kylecorry.sol.math.SolMath.square
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.science.optics.Optics
import com.kylecorry.sol.units.Bearing
import com.kylecorry.trail_sense.shared.camera.AugmentedRealityUtils
import com.kylecorry.trail_sense.shared.camera.GrayscaleMomentFinder
import com.kylecorry.trail_sense.shared.views.CameraView
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.AugmentedRealityCoordinate
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView
import kotlin.math.sqrt

class SunCalibrator {

    private val astro = AstronomyService()

    suspend fun calibrate(view: AugmentedRealityView, camera: CameraView): Float? {
        // TODO: If the sun and actual are too far apart, return null
        val image = camera.previewImage ?: return null
        return onDefault {
            // Scale the image to fit in 100x100
            val scaled = image.resizeToFit(100, 100)
            val scaledWidth = scaled.width
//            val scaledHeight = scaled.height
            if (scaled != image) {
                image.recycle()
            }

            val momentFinder = GrayscaleMomentFinder(240, 5)

            try {
                val moment = momentFinder.getMoment(scaled) ?: return@onDefault null

                // Scale to viewport
                val xPct = moment.x / scaledWidth

                // Determine the actual location of the sun
                // TODO: Determine if perspective projection is necessary
//                val yPct = moment.y / scaledHeight
//                val actualPixel = PixelCoordinate(xPct * view.width, yPct * view.height)
//                val actual = AugmentedRealityCoordinate(Optics.inversePerspectiveProjection(
//                    Vector2(actualPixel.x, view.height - actualPixel.y),
//                    Vector2(
//                        Optics.getFocalLength(camera.fov.first, view.width.toFloat()),
//                        Optics.getFocalLength(camera.fov.second, view.height.toFloat())
//                    ),
//                    Vector2(view.width / 2f, view.height / 2f),
//                    1000f
//                ))
                val actualBearing = (xPct - 0.5f) * camera.fov.first

                // Get the predicted location of the sun relative to the camera
                val predictedLocation = AugmentedRealityCoordinate.fromSpherical(
                    astro.getSunAzimuth(view.location).value,
                    astro.getSunAltitude(view.location),
                    Float.MAX_VALUE,
                    true
                )
                val relativeCoordinate = AugmentedRealityCoordinate(AugmentedRealityUtils.enuToAr(
                    predictedLocation.position,
                    view.rotationMatrix
                ), true)

                // Calculate the bearing difference
                SolMath.deltaAngle(actualBearing, relativeCoordinate.bearing)
            } finally {
                scaled.recycle()
            }
        }
    }

}