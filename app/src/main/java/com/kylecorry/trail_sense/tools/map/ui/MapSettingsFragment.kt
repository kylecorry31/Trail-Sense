package com.kylecorry.trail_sense.tools.map.ui

import android.os.Bundle
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.MapLayersBottomSheet

class MapSettingsFragment : AndromedaPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.map_preferences, rootKey)

        // Layers
        onClick(preference(R.string.pref_map_layer_button)) {
            val prefs = AppServiceRegistry.get<UserPreferences>()
            MapLayersBottomSheet(prefs.map.layerManager).show(this)
        }
    }
}