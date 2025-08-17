package com.kylecorry.trail_sense.shared.map_layers.preferences.ui

import android.os.Bundle
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.trail_sense.R

class MapLayersBottomSheetFragment(private val manager: MapLayerPreferenceManager) :
    AndromedaPreferenceFragment() {
    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?
    ) {
        setPreferencesFromResource(R.xml.empty_preferences, rootKey)
        manager.populatePreferences(preferenceScreen)
    }
}