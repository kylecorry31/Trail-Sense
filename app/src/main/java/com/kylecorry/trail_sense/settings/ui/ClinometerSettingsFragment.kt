package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import androidx.preference.Preference
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences

class ClinometerSettingsFragment : AndromedaPreferenceFragment() {

    private val prefs by lazy { UserPreferences(requireContext()) }
    private val formatter by lazy { FormatService(requireContext()) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.clinometer_preferences, rootKey)

        val baselinePref = preference(R.string.pref_clinometer_baseline_distance_holder)
        updateBaselineSummary(baselinePref)
        baselinePref?.setOnPreferenceClickListener {
            val units = if (prefs.distanceUnits == UserPreferences.DistanceUnits.Meters) {
                listOf(DistanceUnits.Meters, DistanceUnits.Feet)
            } else {
                listOf(DistanceUnits.Feet, DistanceUnits.Meters)
            }
            CustomUiUtils.pickDistance(
                requireContext(),
                units,
                prefs.clinometer.baselineDistance,
                it.title.toString()
            ) { distance, cancelled ->
                if (!cancelled) {
                    if (distance == null || distance.distance <= 0f) {
                        prefs.clinometer.baselineDistance = null
                    } else {
                        prefs.clinometer.baselineDistance = distance
                    }
                    updateBaselineSummary(baselinePref)
                }
            }
            true
        }
    }

    private fun updateBaselineSummary(preference: Preference?) {
        preference?.summary = prefs.clinometer.baselineDistance.let {
            if (it == null) {
                getString(R.string.dash)
            } else {
                formatter.formatDistance(it, Units.getDecimalPlaces(it.units), false)
            }
        }
    }

}