package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences

class ExperimentalSettingsFragment : AndromedaPreferenceFragment() {

    private val prefs by lazy { UserPreferences(requireContext()) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.experimental_preferences, rootKey)

        preference(R.string.pref_experimental_metal_direction)?.isVisible =
            Sensors.hasGyroscope(requireContext())

        preference(R.string.pref_depth_enabled)?.isVisible = Sensors.hasBarometer(requireContext())

        onClick(switch(R.string.pref_experimental_maps)) {
            Package.setComponentEnabled(
                requireContext(),
                "com.kylecorry.trail_sense.AliasMainActivity",
                prefs.navigation.areMapsEnabled
            )
        }

    }
}