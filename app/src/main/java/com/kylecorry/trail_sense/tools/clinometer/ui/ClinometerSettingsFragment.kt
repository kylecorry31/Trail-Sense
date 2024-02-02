package com.kylecorry.trail_sense.tools.clinometer.ui

import android.os.Bundle
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.setupDistanceSetting

class ClinometerSettingsFragment : AndromedaPreferenceFragment() {

    private val prefs by lazy { UserPreferences(requireContext()) }
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.clinometer_preferences, rootKey)

        setupDistanceSetting(
            getString(R.string.pref_clinometer_baseline_distance_holder),
            { prefs.clinometer.baselineDistance },
            { distance ->
                if (distance != null && distance.distance > 0) {
                    prefs.clinometer.baselineDistance = distance
                } else {
                    prefs.clinometer.baselineDistance = null
                }
            },
            DistanceUtils.hikingDistanceUnits
        )
    }
}