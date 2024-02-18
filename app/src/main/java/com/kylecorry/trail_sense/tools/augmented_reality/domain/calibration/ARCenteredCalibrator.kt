package com.kylecorry.trail_sense.tools.augmented_reality.domain.calibration

import com.kylecorry.sol.math.SolMath
import com.kylecorry.trail_sense.shared.views.CameraView
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.AugmentedRealityCoordinate
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView

class ARCenteredCalibrator(private val reference: AugmentedRealityCoordinate) : IARCalibrator {

    override suspend fun calibrateBearing(view: AugmentedRealityView, camera: CameraView): Float {
        val actualReference = AugmentedRealityCoordinate(
            view.getActualPoint(reference.position, true),
            view.isTrueNorth
        )
        return SolMath.deltaAngle(
            view.azimuth,
            actualReference.bearing
        )
    }

}