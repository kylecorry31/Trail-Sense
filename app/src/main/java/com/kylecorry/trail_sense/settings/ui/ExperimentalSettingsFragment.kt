package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import com.kylecorry.andromeda.core.system.PackageUtils
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.andromeda.sense.SensorChecker
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences

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