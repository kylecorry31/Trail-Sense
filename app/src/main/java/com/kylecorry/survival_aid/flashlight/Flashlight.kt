package com.kylecorry.survival_aid.flashlight

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import java.util.*
import androidx.core.content.ContextCompat.getSystemService
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata


class Flashlight(ctx: Context): Observable() {

    var isOn: Boolean = false
        private set(value){
            field = value
            setChanged()
            notifyObservers()
        }

    private val cameraService = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager


    fun on(){
        if (isOn) return
        cameraService.setTorchMode(getRearCameraId(), true)
        isOn = true
    }

    fun off(){
        if (!isOn) return
        cameraService.setTorchMode(getRearCameraId(), false)
        isOn = false
    }

    private fun getRearCameraId(): String {
        val cameraList = cameraService.cameraIdList
        if (cameraList.isEmpty()) return ""
        for (camera in cameraList){
            val hasFlash = cameraService.getCameraCharacteristics(camera).get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
            val facing = cameraService.getCameraCharacteristics(camera).get(CameraCharacteristics.LENS_FACING)
            if(hasFlash != null && hasFlash && facing != null && facing == CameraMetadata.LENS_FACING_BACK){
                return camera
            }

        }
        return cameraList[0]
    }


}