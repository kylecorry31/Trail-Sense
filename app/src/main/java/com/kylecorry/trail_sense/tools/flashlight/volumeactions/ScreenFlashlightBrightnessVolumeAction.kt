package com.kylecorry.trail_sense.tools.flashlight.volumeactions

import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.trail_sense.shared.volume.VolumeAction
import com.kylecorry.trail_sense.tools.flashlight.ui.FragmentToolScreenFlashlight

class ScreenFlashlightBrightnessVolumeAction(
    private val fragment: AndromedaFragment
) : VolumeAction {

    private val screenFlashlightFragment = fragment as? FragmentToolScreenFlashlight

    override fun onButtonPress(): Boolean {
        if (screenFlashlightFragment != null) {
            screenFlashlightFragment.handleVolumeButtonPress(isVolumeUp = true)
            return true
        }
        return false
    }

    override fun onButtonRelease(): Boolean {
        return false
    }
}
