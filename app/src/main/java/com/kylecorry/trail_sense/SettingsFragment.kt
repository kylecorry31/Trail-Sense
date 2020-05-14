package com.kylecorry.trail_sense

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.sensors.SensorChecker

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        // TODO: List open source licenses
        // Austin Andrews - weather icons
        // Michael Irigoyen - moon icons
        val sensorChecker = SensorChecker(requireContext())
        if (!sensorChecker.hasBarometer()) {
            preferenceScreen.removePreferenceRecursively(getString(R.string.pref_weather_category))
        }
    }
}