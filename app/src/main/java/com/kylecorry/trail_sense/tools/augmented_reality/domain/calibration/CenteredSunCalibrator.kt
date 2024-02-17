package com.kylecorry.trail_sense.tools.augmented_reality.domain.calibration

import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath.deltaAngle
import com.kylecorry.trail_sense.shared.views.CameraView
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView

class CenteredSunCalibrator : IARCalibrator {

    private val astro = AstronomyService()

    override suspend fun calibrateBearing(
        view: AugmentedRealityView,
        camera: CameraView
    ): Pair<PixelCoordinate, Float> {
        // TODO: If the sun and actual are too far apart, return null
        return onDefault {
            // TODO: This doesn't factor in declination (this value should always be with declination applied)
            // Get the predicted location of the sun
            val predictedBearing = astro.getSunAzimuth(view.location).value

            // Calculate the bearing difference
            PixelCoordinate(view.width / 2f, view.height / 2f) to deltaAngle(
                view.azimuth,
                predictedBearing
            )

        }
    }

}