package com.kylecorry.trail_sense.volumeactions

import android.content.Context
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightHandler

class FlashlightToggleVolumeAction(private val context: Context) : VolumeAction {
    override fun onButtonPress() {
        val flashlight = FlashlightHandler.getInstance(context)
        flashlight.toggle()
    }

    override fun onButtonRelease() {
        // Do nothing
    }
}