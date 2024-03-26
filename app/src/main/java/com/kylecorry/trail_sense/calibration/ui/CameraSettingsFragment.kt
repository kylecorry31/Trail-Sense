package com.kylecorry.trail_sense.calibration.ui

import android.os.Bundle
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.infrastructure.AugmentedRealityPreferences

class CameraSettingsFragment : AndromedaPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.camera_preferences, rootKey)

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
}