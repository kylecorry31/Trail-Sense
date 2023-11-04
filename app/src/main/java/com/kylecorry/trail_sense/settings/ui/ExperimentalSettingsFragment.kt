package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.sensors.SensorService

class ExperimentalSettingsFragment : AndromedaPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.experimental_preferences, rootKey)

        val sensors = SensorService(requireContext())
        val hasGyro = Sensors.hasGyroscope(requireContext())
        val hasCompass = sensors.hasCompass()

        preference(R.string.pref_experimental_metal_direction)?.isVisible = hasGyro && hasCompass
        preference(R.string.pref_enable_augmented_reality_tool)?.isVisible = hasCompass
    }
}