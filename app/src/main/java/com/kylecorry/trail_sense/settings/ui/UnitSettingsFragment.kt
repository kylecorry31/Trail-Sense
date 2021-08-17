package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.R

class UnitSettingsFragment : AndromedaPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.units_preferences, rootKey)
        preference(R.string.pref_pressure_units)?.isVisible = Sensors.hasBarometer(requireContext())
    }

}