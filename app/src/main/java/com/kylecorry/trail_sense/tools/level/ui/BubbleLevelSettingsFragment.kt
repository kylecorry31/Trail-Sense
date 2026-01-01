package com.kylecorry.trail_sense.tools.level.ui

import android.os.Bundle
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.setupNumberPickerSetting

class BubbleLevelSettingsFragment : AndromedaPreferenceFragment() {

    private val prefs = AppServiceRegistry.get<UserPreferences>().bubbleLevel
    private val formatter = AppServiceRegistry.get<FormatService>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.bubble_level_preferences, rootKey)

        setupNumberPickerSetting(
            getString(R.string.pref_bubble_level_threshold),
            { prefs.threshold.toInt() },
            { prefs.threshold = it.toFloat() },
            minValue = 0,
            maxValue = 10,
            formatValue = { formatter.formatDegrees(it.toFloat()) }
        )
    }
}
