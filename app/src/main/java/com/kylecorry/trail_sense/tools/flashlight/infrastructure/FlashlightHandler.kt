package com.kylecorry.trail_sense.tools.flashlight.infrastructure

import android.content.Context
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraManager.TorchCallback
import android.os.Handler
import android.os.Looper
import androidx.core.content.getSystemService
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightState
import com.kylecorry.trailsensecore.infrastructure.flashlight.Flashlight
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer


class FlashlightHandler(private val context: Context) : IFlashlightHandler {

    private val torchCallback: TorchCallback

    init {
        torchCallback = object : TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                try {
                    super.onTorchModeChanged(cameraId, enabled)
                    if (!enabled && FlashlightService.isOn(context)) {
                        off()
                    }

                    if (enabled && !FlashlightService.isOn(context) && !SosService.isOn(context) && !StrobeService.isOn(context)){
                        on()
                    }
                } catch (e: Exception){
                    // Ignore
                }
            }
        }
        initialize()
    }

    override fun initialize() {
        registerTorchCallback()
    }

    override fun release() {
        unregisterTorchCallback()
        off()
    }

    override fun on() {
        SosService.stop(context)
        StrobeService.stop(context)
        FlashlightService.start(context)
    }

    override fun off() {
        SosService.stop(context)
        StrobeService.stop(context)
        FlashlightService.stop(context)
    }

    override fun sos() {
        StrobeService.stop(context)
        FlashlightService.stop(context)
        SosService.start(context)
    }

    override fun strobe() {
        SosService.stop(context)
        FlashlightService.stop(context)
        StrobeService.start(context)
    }

    override fun set(state: FlashlightState) {
        when (state) {
            FlashlightState.Off -> off()
            FlashlightState.On -> on()
            FlashlightState.SOS -> sos()
            FlashlightState.Strobe -> strobe()
        }
    }

    override fun getState(): FlashlightState {
        return when {
            FlashlightService.isOn(context) -> FlashlightState.On
            SosService.isOn(context) -> FlashlightState.SOS
            StrobeService.isOn(context) -> FlashlightState.Strobe
            else -> FlashlightState.Off
        }
    }

    override fun isAvailable(): Boolean {
        return Flashlight.hasFlashlight(context)
    }

    private fun registerTorchCallback(){
        try {
            context.getSystemService<CameraManager>()?.registerTorchCallback(
                torchCallback, Handler(
                    Looper.getMainLooper()
                )
            )
        } catch (e: Exception){

        }
    }

    private fun unregisterTorchCallback(){
        try {
            context.getSystemService<CameraManager>()?.unregisterTorchCallback(torchCallback)
        } catch (e: Exception){

        }
    }

}