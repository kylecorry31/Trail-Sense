package com.kylecorry.trail_sense.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.kylecorry.trail_sense.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        // TODO: List open source licenses
        // Austin Andrews - weather icons
        // Michael Irigoyen - moon icons
    }
}