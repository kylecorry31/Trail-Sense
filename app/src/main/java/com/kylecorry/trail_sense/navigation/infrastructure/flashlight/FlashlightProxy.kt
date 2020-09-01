package com.kylecorry.trail_sense.navigation.infrastructure.flashlight

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import androidx.core.content.getSystemService
import java.lang.Exception

class FlashlightProxy(private val context: Context) {

    private val cameraService = context.getSystemService<CameraManager>()

    fun on() {
        if (!hasFlashlight(context)){
            return
        }
        try {
            cameraService?.setTorchMode(getRearCameraId(context), true)
        } catch (e: Exception) {
            // No flash, ignoring
        }
    }

    fun off() {
        try {
            cameraService?.setTorchMode(getRearCameraId(context), false)
        } catch (e: Exception) {
            // No flash, ignoring
        }
    }

    companion object {

        fun hasFlashlight(context: Context): Boolean {
            val cs = context.getSystemService<CameraManager>()
            val rearCamera = getRearCameraId(context)
            if (rearCamera.isEmpty() || cs == null){
                return false
            }

            val hasFlash = cs.getCameraCharacteristics(rearCamera)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
            val facing = cs.getCameraCharacteristics(rearCamera)
                .get(CameraCharacteristics.LENS_FACING)

            return hasFlash != null && hasFlash && facing != null && facing == CameraMetadata.LENS_FACING_BACK
        }

        private fun getRearCameraId(context: Context): String {
            val cs = context.getSystemService<CameraManager>()
            val cameraList = cs?.cameraIdList
            if (cameraList == null || cameraList.isEmpty()) return ""
            for (camera in cameraList) {
                val hasFlash = cs.getCameraCharacteristics(camera)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                val facing = cs.getCameraCharacteristics(camera)
                    .get(CameraCharacteristics.LENS_FACING)
                if (hasFlash != null && hasFlash && facing != null && facing == CameraMetadata.LENS_FACING_BACK) {
                    return camera
                }

            }
            return cameraList[0]
        }
    }

}