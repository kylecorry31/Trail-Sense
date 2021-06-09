package com.kylecorry.trail_sense.volumeactions

import android.content.Context
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightState
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightHandler

class FlashlightToggleVolumeAction(private val context: Context) : VolumeAction {
    override fun onButtonDown() {
        val flashlight = FlashlightHandler.getInstance(context)
        if (flashlight.getState() == FlashlightState.On) {
            flashlight.off()
        } else {
            flashlight.on()
        }
    }

    override fun onButtonUp() {
        // Do nothing
    }
}