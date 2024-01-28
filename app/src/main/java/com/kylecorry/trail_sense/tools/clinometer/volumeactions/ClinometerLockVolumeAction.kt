package com.kylecorry.trail_sense.tools.clinometer.volumeactions

import com.kylecorry.trail_sense.shared.VolumeAction
import com.kylecorry.trail_sense.tools.clinometer.ui.ClinometerFragment

class ClinometerLockVolumeAction(private val fragment: ClinometerFragment) : VolumeAction {
    override fun onButtonPress() {
        fragment.onTouchDown()
    }

    override fun onButtonRelease() {
        fragment.onTouchUp()
    }
}