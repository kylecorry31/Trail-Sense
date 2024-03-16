package com.kylecorry.trail_sense.tools.augmented_reality.ui

import android.os.Bundle
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.infrastructure.AugmentedRealityPreferences
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.debugging.isDebug
import com.kylecorry.trail_sense.shared.preferences.setupDistanceSetting
import com.kylecorry.trail_sense.shared.sensors.compass.CompassSource
import com.kylecorry.trail_sense.shared.sensors.providers.CompassProvider

class AugmentedRealitySettingsFragment : AndromedaPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.augmented_reality_preferences, rootKey)

        val userPrefs = UserPreferences(requireContext())

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
            DistanceUtils.hikingDistanceUnits
        )

        preference(R.string.pref_view_distance_paths_holder)?.isVisible = isDebug()

        preference(R.string.pref_ar_use_gyro_only_after_calibration)?.isVisible =
            Sensors.hasGyroscope(requireContext())

        // Set the projection type options
        val names = mapOf(
            AugmentedRealityPreferences.ProjectionType.EstimatedIntrinsics to "${getString(R.string.projection_estimated_intrinsics)} (${
                getString(R.string.default_string)
            })",
            AugmentedRealityPreferences.ProjectionType.ManufacturerIntrinsics to getString(R.string.projection_manufacturer_intrinsics),
            AugmentedRealityPreferences.ProjectionType.Perspective to getString(R.string.projection_perspective),
            AugmentedRealityPreferences.ProjectionType.Linear to getString(R.string.projection_linear)
        )
        val ids = names.map { it.key.id }

        val projectionType = list(R.string.pref_augmented_reality_mapper)
        projectionType?.entries = names.values.toTypedArray()
        projectionType?.entryValues = ids.toTypedArray()
    }

    private fun relative(distance: Distance, prefs: UserPreferences): Distance {
        return distance.convertTo(prefs.baseDistanceUnits).toRelativeDistance()
    }
}