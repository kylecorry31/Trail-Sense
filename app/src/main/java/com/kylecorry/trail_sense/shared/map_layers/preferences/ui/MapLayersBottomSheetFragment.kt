package com.kylecorry.trail_sense.shared.map_layers.preferences.ui

import android.os.Bundle
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity

class MapLayersBottomSheetFragment(
    private val manager: MapLayerPreferenceManager,
    private val activity: MainActivity
) :
    AndromedaPreferenceFragment() {
    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?
    ) {
        setPreferencesFromResource(R.xml.empty_preferences, rootKey)
        manager.onScrollToTop = {
            listView?.scrollToPosition(0)
        }
        manager.populatePreferences(preferenceScreen, activity)
    }
}