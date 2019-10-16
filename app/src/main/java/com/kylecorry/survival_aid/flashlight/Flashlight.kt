package com.kylecorry.survival_aid.flashlight

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import java.util.*
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata


class Flashlight(ctx: Context, initialMode: FlashlightMode = NormalFlashlightMode()): Observable() {

    var mode: FlashlightMode = initialMode
        set(value){
            val wasOn = isOn
            off()
            field = value
            if (wasOn) on()
        }

    var isOn: Boolean = false
        private set(value){
            field = value
            setChanged()
            notifyObservers()
        }

    internal var isBulbOn: Boolean = false
        private set

    private val cameraService = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager


    fun on(){
        if (isOn) return
        mode.on(this)
        isOn = true
    }

    fun off(){
        if (!isOn) return
        mode.off(this)
        isOn = false
    }

    internal fun bulbOn(){
        cameraService.setTorchMode(getRearCameraId(), true)
        isBulbOn = true
    }

    internal fun bulbOff(){
        cameraService.setTorchMode(getRearCameraId(), false)
        isBulbOn = false
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