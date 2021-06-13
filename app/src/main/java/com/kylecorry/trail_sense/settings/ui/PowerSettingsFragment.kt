package com.kylecorry.trail_sense.settings.ui

import android.os.Build
import android.os.Bundle
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.LowPowerMode
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tiles.TileManager

class PowerSettingsFragment : CustomPreferenceFragment() {

    private val prefs by lazy { UserPreferences(requireContext()) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.power_preferences, rootKey)

        onClick(switch(R.string.pref_low_power_mode)) {
            // TODO: Move preference to it's own repo
            if (prefs.isLowPowerModeOn) {
                prefs.power.userEnabledLowPower = true
                LowPowerMode(requireContext()).enable(requireActivity())
            } else {
                prefs.power.userEnabledLowPower = false
                LowPowerMode(requireContext()).disable(requireActivity())
            }
        }


        switch(R.string.pref_tiles_enabled)?.isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
        onClick(switch(R.string.pref_tiles_enabled)){
            TileManager().setTilesEnabled(requireContext(), prefs.power.areTilesEnabled)
        }

    }

}