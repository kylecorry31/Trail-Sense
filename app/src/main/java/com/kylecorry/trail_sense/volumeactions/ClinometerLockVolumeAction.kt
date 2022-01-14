package com.kylecorry.trail_sense.volumeactions

import com.kylecorry.trail_sense.shared.PressState
import com.kylecorry.trail_sense.tools.clinometer.ui.ClinometerFragment

class ClinometerLockVolumeAction(private val fragment: ClinometerFragment) : VolumeAction {
    override fun onButtonPress() {
        fragment.updateLockState(PressState.Down)
    }

    override fun onButtonRelease() {
        fragment.updateLockState(PressState.Up)
    }
}