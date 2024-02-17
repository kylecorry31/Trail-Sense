package com.kylecorry.trail_sense.tools.augmented_reality.domain.calibration

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.shared.views.CameraView
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView

interface IARCalibrator {
    /**
     * Calibrate the bearing of the AR view
     * @param view The AR view
     * @param camera The camera view
     * @return The pixel coordinate of the calibration point and the bearing calibration in degrees
     */
    suspend fun calibrateBearing(
        view: AugmentedRealityView,
        camera: CameraView
    ): Pair<PixelCoordinate, Float>?
}