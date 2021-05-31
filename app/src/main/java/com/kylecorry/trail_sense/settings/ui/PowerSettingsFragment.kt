package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.LowPowerMode
import com.kylecorry.trail_sense.shared.UserPreferences

class PowerSettingsFragment : CustomPreferenceFragment() {

    private val prefs by lazy { UserPreferences(requireContext()) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.power_preferences, rootKey)

        // TODO: Navigate back to this page when low power mode is activated
        onClick(switch(R.string.pref_low_power_mode)) {
            // TODO: Move preference to it's own repo
            if (prefs.isLowPowerModeOn) {
                LowPowerMode(requireContext()).enable(requireActivity())
            } else {
                LowPowerMode(requireContext()).disable(requireActivity())
            }
        }

    }

}