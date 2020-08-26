package com.kylecorry.trail_sense.flashlight.infrastructure

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import androidx.core.content.getSystemService
import java.lang.Exception

class Flashlight(context: Context) {

    private val cameraService = context.getSystemService<CameraManager>()

    fun on() {
        try {
            cameraService?.setTorchMode(getRearCameraId(), true)
        } catch (e: Exception) {
            // No flash, ignoring
        }
    }

    fun off() {
        try {
            cameraService?.setTorchMode(getRearCameraId(), false)
        } catch (e: Exception) {
            // No flash, ignoring
        }
    }

    private fun getRearCameraId(): String {
        val cameraList = cameraService?.cameraIdList
        if (cameraList == null || cameraList.isEmpty()) return ""
        for (camera in cameraList) {
            val hasFlash = cameraService?.getCameraCharacteristics(camera)
                ?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
            val facing = cameraService?.getCameraCharacteristics(camera)
                ?.get(CameraCharacteristics.LENS_FACING)
            if (hasFlash != null && hasFlash && facing != null && facing == CameraMetadata.LENS_FACING_BACK) {
                return camera
            }

        }
        return cameraList[0]
    }

}