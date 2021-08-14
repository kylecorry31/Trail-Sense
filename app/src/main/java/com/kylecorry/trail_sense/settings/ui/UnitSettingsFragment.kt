package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorChecker

class UnitSettingsFragment : AndromedaPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.units_preferences, rootKey)
        val sensorChecker = SensorChecker(requireContext())
        preference(R.string.pref_pressure_units)?.isVisible = sensorChecker.hasBarometer()
    }

}