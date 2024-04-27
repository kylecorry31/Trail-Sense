package com.kylecorry.trail_sense.tools.flashlight.volumeactions

import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.trail_sense.shared.volume.VolumeAction
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem
import com.kylecorry.trail_sense.tools.flashlight.ui.FragmentToolFlashlight

class FlashlightToggleVolumeAction(
    private val fragment: AndromedaFragment
) : VolumeAction {

    private val flashlightFragment = fragment as? FragmentToolFlashlight

    override fun onButtonPress(): Boolean {
        if (flashlightFragment == null) {
            val flashlight = FlashlightSubsystem.getInstance(fragment.requireContext())
            flashlight.toggle()
        } else {
            flashlightFragment.toggle()
        }
        return true
    }

    override fun onButtonRelease(): Boolean {
        // Do nothing
        return true
    }
}