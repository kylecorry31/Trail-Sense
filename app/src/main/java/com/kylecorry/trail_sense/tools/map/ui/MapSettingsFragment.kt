package com.kylecorry.trail_sense.tools.map.ui

import android.os.Bundle
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences

class MapSettingsFragment : AndromedaPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.map_preferences, rootKey)

        // Layers
        val prefs = AppServiceRegistry.get<UserPreferences>()
        prefs.map.layerManager.populatePreferences(preferenceScreen)
    }
}