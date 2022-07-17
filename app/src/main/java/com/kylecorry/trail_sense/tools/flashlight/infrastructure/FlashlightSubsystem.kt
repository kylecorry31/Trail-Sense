package com.kylecorry.trail_sense.tools.flashlight.infrastructure

import android.content.Context
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraManager.TorchCallback
import android.os.Handler
import android.os.Looper
import androidx.core.content.getSystemService
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.torch.Torch
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.infrastructure.FlashlightPreferenceRepo
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightState
import java.time.Instant


class FlashlightSubsystem private constructor(private val context: Context) : IFlashlightSubsystem {

    private val torchCallback: TorchCallback
    private val cache by lazy { Preferences(context) }
    private val prefs by lazy { UserPreferences(context) }
    private val handler: Handler
    private var isTurningOff = false
    private val flashlightSettings by lazy { FlashlightPreferenceRepo(context) }

    init {
        torchCallback = object : TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                try {
                    super.onTorchModeChanged(cameraId, enabled)
                    if (!flashlightSettings.toggleWithSystem) {
                        return
                    }
                    if (isTurningOff) {
                        return
                    }

                    if (!enabled && FlashlightService.isRunning) {
                        off()
                    }

                    if (enabled && !FlashlightService.isRunning && !SosService.isRunning && !StrobeService.isRunning) {
                        on(false)
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
        clearTimeout()
        unregisterTorchCallback()
        SosService.stop(context)
        StrobeService.stop(context)
        FlashlightService.stop(context)
    }

    override fun on(handleTimeout: Boolean) {
        clearTimeout()
        if (handleTimeout) {
            setTimeout()
        }
        SosService.stop(context)
        StrobeService.stop(context)
        FlashlightService.start(context)
    }

    override fun off() {
        clearTimeout()
        SosService.stop(context)
        StrobeService.stop(context)
        FlashlightService.stop(context)
        isTurningOff = true
        forceOff(200)
    }

    override fun toggle(handleTimeout: Boolean) {
        if (getState() == FlashlightState.On) {
            off()
        } else {
            on(handleTimeout)
        }
    }

    private fun forceOff(millis: Long) {
        val increment = 20L
        if ((millis - increment) < 0) {
            isTurningOff = false
            return
        }
        handler.postDelayed({
            val flashlight = Torch(context)
            flashlight.off()
            forceOff(millis - increment)
        }, increment)
    }

    override fun sos(handleTimeout: Boolean) {
        clearTimeout()
        if (handleTimeout) {
            setTimeout()
        }
//        unregisterTorchCallback()
        StrobeService.stop(context)
        FlashlightService.stop(context)
        SosService.start(context)
    }

    override fun strobe(handleTimeout: Boolean) {
        clearTimeout()
        if (handleTimeout) {
            setTimeout()
        }
//        unregisterTorchCallback()
        SosService.stop(context)
        FlashlightService.stop(context)
        StrobeService.start(context)
    }

    override fun set(state: FlashlightState, handleTimeout: Boolean) {
        when (state) {
            FlashlightState.Off -> off()
            FlashlightState.On -> on(handleTimeout)
            FlashlightState.SOS -> sos(handleTimeout)
            FlashlightState.Strobe -> strobe(handleTimeout)
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
        return Torch.isAvailable(context)
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

    private fun setTimeout() {
        if (prefs.flashlight.shouldTimeout) {
            cache.putInstant(
                context.getString(R.string.pref_flashlight_timeout_instant),
                Instant.now().plus(prefs.flashlight.timeout)
            )
        } else {
            clearTimeout()
        }
    }

    private fun clearTimeout(){
        cache.remove(context.getString(R.string.pref_flashlight_timeout_instant))
    }

    private fun unregisterTorchCallback() {
        try {
            context.getSystemService<CameraManager>()?.unregisterTorchCallback(torchCallback)
        } catch (e: Exception) {

        }
    }

    companion object {
        private var instance: FlashlightSubsystem? = null
        fun getInstance(context: Context): FlashlightSubsystem {
            if (instance == null) {
                instance = FlashlightSubsystem(context.applicationContext)
            }
            return instance!!
        }
    }

}