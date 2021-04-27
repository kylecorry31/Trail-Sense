package com.kylecorry.trail_sense.tools.flashlight.infrastructure

import android.content.Context
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraManager.TorchCallback
import android.os.Handler
import android.os.Looper
import androidx.core.content.getSystemService
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightState
import com.kylecorry.trailsensecore.infrastructure.flashlight.Flashlight


class FlashlightHandler private constructor(private val context: Context) : IFlashlightHandler {

    private val torchCallback: TorchCallback
    private val handler: Handler
    private var isTurningOff = false

    init {
        torchCallback = object : TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                try {
                    super.onTorchModeChanged(cameraId, enabled)
                    if (isTurningOff) {
                        return
                    }

                    if (!enabled && FlashlightService.isRunning) {
                        off()
                    }

                    if (enabled && !FlashlightService.isRunning && !SosService.isRunning && !StrobeService.isRunning) {
                        on()
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
        handler = Handler(Looper.getMainLooper())
        initialize()
    }

    override fun initialize() {
        registerTorchCallback()
    }

    override fun release() {
        unregisterTorchCallback()
        SosService.stop(context)
        StrobeService.stop(context)
        FlashlightService.stop(context)
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
        isTurningOff = true
        forceOff(200)
    }

    private fun forceOff(millis: Long) {
        val increment = 20L
        if ((millis - increment) < 0) {
            isTurningOff = false
            return
        }
        handler.postDelayed({
            val flashlight = Flashlight(context)
            flashlight.off()
            forceOff(millis - increment)
        }, increment)
    }

    override fun sos() {
//        unregisterTorchCallback()
        StrobeService.stop(context)
        FlashlightService.stop(context)
        SosService.start(context)
    }

    override fun strobe() {
//        unregisterTorchCallback()
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
            FlashlightService.isRunning -> FlashlightState.On
            SosService.isRunning -> FlashlightState.SOS
            StrobeService.isRunning -> FlashlightState.Strobe
            else -> FlashlightState.Off
        }
    }

    override fun isAvailable(): Boolean {
        return Flashlight.hasFlashlight(context)
    }

    private fun registerTorchCallback() {
        try {
            context.getSystemService<CameraManager>()?.registerTorchCallback(
                torchCallback, Handler(
                    Looper.getMainLooper()
                )
            )
        } catch (e: Exception) {

        }
    }

    private fun unregisterTorchCallback() {
        try {
            context.getSystemService<CameraManager>()?.unregisterTorchCallback(torchCallback)
        } catch (e: Exception) {

        }
    }

    companion object {
        private var instance: FlashlightHandler? = null
        fun getInstance(context: Context): FlashlightHandler {
            if (instance == null) {
                instance = FlashlightHandler(context.applicationContext)
            }
            return instance!!
        }
    }

}