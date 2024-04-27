package com.kylecorry.trail_sense.tools.augmented_reality.domain.mapper

import com.kylecorry.andromeda.camera.ICamera
import com.kylecorry.trail_sense.settings.infrastructure.CameraPreferences

class CameraAnglePixelMapperFactory {
    fun getMapper(
        type: CameraPreferences.ProjectionType,
        camera: ICamera
    ): CameraAnglePixelMapper {
        val perspective = SimplePerspectiveCameraAnglePixelMapper()

        return when (type) {
            CameraPreferences.ProjectionType.EstimatedIntrinsics -> CalibratedCameraAnglePixelMapper(
                camera,
                perspective,
                useManufacturerCalibration = false
            )

            CameraPreferences.ProjectionType.ManufacturerIntrinsics -> CalibratedCameraAnglePixelMapper(
                camera,
                perspective,
                useManufacturerCalibration = true
            )

            CameraPreferences.ProjectionType.Perspective -> perspective
            CameraPreferences.ProjectionType.Linear -> LinearCameraAnglePixelMapper()
        }
    }
}