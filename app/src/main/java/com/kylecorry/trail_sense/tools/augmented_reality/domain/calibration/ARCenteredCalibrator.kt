package com.kylecorry.trail_sense.tools.augmented_reality.domain.calibration

import com.kylecorry.sol.math.SolMath
import com.kylecorry.trail_sense.shared.views.CameraView
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.AugmentedRealityCoordinate
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView

class ARCenteredCalibrator(private val reference: AugmentedRealityCoordinate) : IARCalibrator {

    override suspend fun calibrateBearing(view: AugmentedRealityView, camera: CameraView): Float {
        // TODO: Make sure the azimuth and reference are adjusted for declination if not True north - this calculation assumes it is True north
        return SolMath.deltaAngle(
            view.azimuth,
            reference.bearing
        )
    }

}