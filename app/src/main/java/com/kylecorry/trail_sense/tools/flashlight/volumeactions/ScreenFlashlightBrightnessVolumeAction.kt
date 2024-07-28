package com.kylecorry.trail_sense.tools.flashlight.volumeactions

import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.trail_sense.shared.volume.VolumeAction
import com.kylecorry.trail_sense.tools.flashlight.ui.FragmentToolScreenFlashlight

class ScreenFlashlightBrightnessVolumeAction(
    private val fragment: AndromedaFragment
) : VolumeAction {

    private val screenFlashlightFragment = fragment as? FragmentToolScreenFlashlight

    override fun onButtonPress(isUpButton: Boolean): Boolean {
        screenFlashlightFragment?.handleVolumeButtonPress(isVolumeUp = isUpButton)
        return screenFlashlightFragment != null
    }

    override fun onButtonRelease(isUpButton: Boolean): Boolean {
        return screenFlashlightFragment != null
    }
}
