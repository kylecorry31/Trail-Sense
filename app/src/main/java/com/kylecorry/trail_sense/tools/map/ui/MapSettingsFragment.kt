package com.kylecorry.trail_sense.tools.map.ui

import android.os.Bundle
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.MapLayerPreferences
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.MapLayerPreferenceManager

class MapSettingsFragment : AndromedaPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.map_preferences, rootKey)

        // Layers
        val layerManager = MapLayerPreferenceManager(
            "map", listOf(
                MapLayerPreferences.photoMaps(requireContext(), defaultOpacity = 100),
                MapLayerPreferences.contours(requireContext(), enabledByDefault = true)
            )
        )
        layerManager.populatePreferences(preferenceScreen)
    }
}