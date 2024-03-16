package com.kylecorry.trail_sense.tools.augmented_reality.domain.mapper

import com.kylecorry.andromeda.camera.ICamera
import com.kylecorry.trail_sense.settings.infrastructure.AugmentedRealityPreferences

class CameraAnglePixelMapperFactory {
    fun getMapper(
        type: AugmentedRealityPreferences.ProjectionType,
        camera: ICamera
    ): CameraAnglePixelMapper {
        val perspective = SimplePerspectiveCameraAnglePixelMapper()

        return when (type) {
            AugmentedRealityPreferences.ProjectionType.EstimatedIntrinsics -> CalibratedCameraAnglePixelMapper(
                camera,
                perspective,
                useManufacturerCalibration = false
            )

            AugmentedRealityPreferences.ProjectionType.ManufacturerIntrinsics -> CalibratedCameraAnglePixelMapper(
                camera,
                perspective,
                useManufacturerCalibration = true
            )

            AugmentedRealityPreferences.ProjectionType.Perspective -> perspective
            AugmentedRealityPreferences.ProjectionType.Linear -> LinearCameraAnglePixelMapper()
        }
    }
}