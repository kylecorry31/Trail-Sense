package com.kylecorry.survival_aid.flashlight

import android.content.Context
import java.util.*
import androidx.core.content.ContextCompat.getSystemService
import android.hardware.camera2.CameraManager



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
        return cameraList[0]
    }


}