package com.kylecorry.trail_sense.tools.clinometer.volumeactions

import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.trail_sense.shared.volume.VolumeAction
import com.kylecorry.trail_sense.tools.clinometer.ui.ClinometerFragment

class ClinometerLockVolumeAction(fragment: AndromedaFragment) : VolumeAction {

    private val clinometerFragment = fragment as? ClinometerFragment

    override fun onButtonPress(isUpButton: Boolean): Boolean {
        clinometerFragment?.onTouchDown()
        return clinometerFragment != null
    }

    override fun onButtonRelease(isUpButton: Boolean): Boolean {
        clinometerFragment?.onTouchUp()
        return clinometerFragment != null
    }
}