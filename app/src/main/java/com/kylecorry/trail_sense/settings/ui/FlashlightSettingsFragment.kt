package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import com.kylecorry.trail_sense.R

class FlashlightSettingsFragment : AndromedaPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.flashlight_preferences, rootKey)
    }

}