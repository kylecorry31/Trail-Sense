package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorChecker
import com.kylecorry.trailsensecore.infrastructure.system.PackageUtils

class ExperimentalSettingsFragment : AndromedaPreferenceFragment() {

    private val prefs by lazy { UserPreferences(requireContext()) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.experimental_preferences, rootKey)

        val sensorChecker = SensorChecker(requireContext())
        preference(R.string.pref_experimental_metal_direction)?.isVisible =
            sensorChecker.hasGyroscope()

        preference(R.string.pref_depth_enabled)?.isVisible = sensorChecker.hasBarometer()

        onClick(switch(R.string.pref_experimental_maps)) {
            PackageUtils.setComponentEnabled(
                requireContext(),
                "com.kylecorry.trail_sense.AliasMainActivity",
                prefs.navigation.areMapsEnabled
            )
        }

    }
}