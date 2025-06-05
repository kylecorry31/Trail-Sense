package com.kylecorry.trail_sense.tools.turn_back.ui

import android.os.Bundle
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.trail_sense.R

class TurnBackSettingsFragment : AndromedaPreferenceFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.turn_back_preferences, rootKey)
    }
}