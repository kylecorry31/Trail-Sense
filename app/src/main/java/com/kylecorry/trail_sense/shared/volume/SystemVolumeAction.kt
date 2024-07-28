package com.kylecorry.trail_sense.shared.volume

import com.kylecorry.andromeda.fragments.AndromedaFragment

class SystemVolumeAction(fragment: AndromedaFragment): VolumeAction {
    override fun onButtonPress(isUpButton: Boolean): Boolean {
        return false
    }

    override fun onButtonRelease(isUpButton: Boolean): Boolean {
        return false
    }
}