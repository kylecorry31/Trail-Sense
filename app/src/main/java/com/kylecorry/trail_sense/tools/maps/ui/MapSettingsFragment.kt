package com.kylecorry.trail_sense.tools.maps.ui

import android.os.Bundle
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils

class MapSettingsFragment : AndromedaPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.map_preferences, rootKey)

        val reduceResolutionPreference = switch(R.string.pref_low_resolution_maps)

        onClick(reduceResolutionPreference) {
            if (reduceResolutionPreference?.isChecked == true) {
                return@onClick
            }

            CustomUiUtils.disclaimer(
                requireContext(),
                getString(R.string.reduce_map_resolution),
                getString(R.string.reduce_map_resolution_crop_disclaimer),
                getString(R.string.reduce_map_resolution_crop_disclaimer_shown),
                cancelText = null
            )
        }

    }

}