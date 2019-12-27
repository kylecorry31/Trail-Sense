package com.kylecorry.survival_aid.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.kylecorry.survival_aid.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}