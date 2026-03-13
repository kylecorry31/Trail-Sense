package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.UserPreferences

class BeaconSettingsFragment : AndromedaPreferenceFragment() {

    private val prefs by lazy { UserPreferences(requireContext()) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.beacons_preferences, rootKey)

        val prefBeaconDefaultColor = preference(R.string.pref_beacon_default_color)
        prefBeaconDefaultColor?.icon?.setTint(
            prefs.beacons.defaultBeaconColor.color
        )

        prefBeaconDefaultColor?.setOnPreferenceClickListener {
            CustomUiUtils.pickColor(
                requireContext(),
                prefs.beacons.defaultBeaconColor,
                it.title.toString()
            ) {
                if (it != null) {
                    prefs.beacons.defaultBeaconColor = it
                    prefBeaconDefaultColor.icon?.setTint(it.color)
                }
            }
            true
        }
    }
}
