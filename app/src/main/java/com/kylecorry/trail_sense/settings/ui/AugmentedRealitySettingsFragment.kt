package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences

class AugmentedRealitySettingsFragment : AndromedaPreferenceFragment() {

    private val formatService by lazy { FormatService.getInstance(requireContext()) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.augmented_reality_preferences, rootKey)

        val userPrefs = UserPreferences(requireContext())
        val prefNearbyRadius = preference(R.string.pref_nearby_radius_holder)

        prefNearbyRadius?.setOnPreferenceClickListener {
            val units = formatService.sortDistanceUnits(DistanceUtils.hikingDistanceUnits)

            CustomUiUtils.pickDistance(
                requireContext(),
                units,
                getViewDistance(userPrefs),
                it.title.toString()
            ) { distance, _ ->
                if (distance != null && distance.distance > 0) {
                    userPrefs.augmentedReality.viewDistance = distance.meters().distance
                    prefNearbyRadius.summary = getNearbyRadiusString(userPrefs)
                }
            }
            true
        }

        prefNearbyRadius?.summary = getNearbyRadiusString(userPrefs)
    }

    private fun getViewDistance(prefs: UserPreferences): Distance {
        return Distance.meters(prefs.augmentedReality.viewDistance)
            .convertTo(prefs.baseDistanceUnits)
            .toRelativeDistance()
    }

    private fun getNearbyRadiusString(prefs: UserPreferences): String {
        val distance = getViewDistance(prefs)

        return formatService.formatDistance(distance, Units.getDecimalPlaces(distance.units))
    }

}