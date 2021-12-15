package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.trail_sense.R

class ClinometerSettingsFragment : AndromedaPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.clinometer_preferences, rootKey)
    }

}