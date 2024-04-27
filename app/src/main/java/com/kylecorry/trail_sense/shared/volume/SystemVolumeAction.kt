package com.kylecorry.trail_sense.shared.volume

import com.kylecorry.andromeda.fragments.AndromedaFragment

class SystemVolumeAction(fragment: AndromedaFragment): VolumeAction {
    override fun onButtonPress(): Boolean {
        return false
    }

    override fun onButtonRelease(): Boolean {
        return false
    }
}