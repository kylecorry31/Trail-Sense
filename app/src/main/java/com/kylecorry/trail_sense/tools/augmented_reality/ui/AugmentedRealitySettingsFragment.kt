package com.kylecorry.trail_sense.tools.augmented_reality.ui

import android.os.Bundle
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.setupDistanceSetting

class AugmentedRealitySettingsFragment : AndromedaPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.augmented_reality_preferences, rootKey)

        val userPrefs = UserPreferences(requireContext())
        val formattedMaxDistance = FormatService.getInstance(requireContext())
            .formatDistance(
                relative(
                    Distance.meters(userPrefs.augmentedReality.maxRecommendedPathViewDistanceMeters),
                    userPrefs
                )
            )

        setupDistanceSetting(
            getString(R.string.pref_view_distance_beacons_holder),
            { relative(Distance.meters(userPrefs.augmentedReality.beaconViewDistance), userPrefs) },
            { distance ->
                if (distance != null && distance.distance > 0) {
                    userPrefs.augmentedReality.beaconViewDistance = distance.meters().distance
                }
            },
            DistanceUtils.hikingDistanceUnits
        )

        setupDistanceSetting(
            getString(R.string.pref_view_distance_paths_holder),
            { relative(Distance.meters(userPrefs.augmentedReality.pathViewDistance), userPrefs) },
            { distance ->
                if (distance != null && distance.distance > 0) {
                    userPrefs.augmentedReality.pathViewDistance = distance.meters().distance
                }
            },
            DistanceUtils.hikingDistanceUnits,
            description = getString(
                R.string.path_distance_performance_disclaimer,
                formattedMaxDistance
            )
        )

        preference(R.string.pref_ar_use_gyro_only_after_calibration)?.isVisible =
            Sensors.hasGyroscope(requireContext())
    }

    private fun relative(distance: Distance, prefs: UserPreferences): Distance {
        return distance.convertTo(prefs.baseDistanceUnits).toRelativeDistance()
    }
}