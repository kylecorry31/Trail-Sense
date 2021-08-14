package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import com.kylecorry.trail_sense.R

class MapSettingsFragment : AndromedaPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.map_preferences, rootKey)
    }

}