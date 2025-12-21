package com.kylecorry.trail_sense.tools.level.ui

import android.os.Bundle
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.infrastructure.BubbleLevelPreferences
import com.kylecorry.trail_sense.shared.preferences.setupThresholdSetting

class BubbleLevelSettingsFragment : AndromedaPreferenceFragment() {

    private val prefs by lazy { BubbleLevelPreferences(requireContext()) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.bubble_level_preferences, rootKey)

        setupThresholdSetting(
            getString(R.string.pref_bubble_level_threshold),
            { prefs.threshold.toInt() },
            { prefs.threshold = it.toFloat() },
            minValue = 1,
            maxValue = 10,
            formatValue = { getString(R.string.degree_format, it.toFloat()) }
        )
    }
}
