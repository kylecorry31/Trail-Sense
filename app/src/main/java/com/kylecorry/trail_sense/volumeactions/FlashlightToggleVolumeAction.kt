package com.kylecorry.trail_sense.volumeactions

import android.content.Context
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem
import com.kylecorry.trail_sense.tools.flashlight.ui.FragmentToolFlashlight

class FlashlightToggleVolumeAction(
    private val context: Context,
    private val fragment: FragmentToolFlashlight? = null
) : VolumeAction {
    override fun onButtonPress() {
        if (fragment == null) {
            val flashlight = FlashlightSubsystem.getInstance(context)
            flashlight.toggle()
        } else {
            fragment.toggle()
        }
    }

    override fun onButtonRelease() {
        // Do nothing
    }
}