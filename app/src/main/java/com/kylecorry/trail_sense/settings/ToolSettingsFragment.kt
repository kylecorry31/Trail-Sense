package com.kylecorry.trail_sense.settings

import android.os.Bundle
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.BacktrackScheduler

class ToolSettingsFragment : CustomPreferenceFragment() {

    private lateinit var prefs: UserPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.tools_preferences, rootKey)
        val userPrefs = UserPreferences(requireContext())
        prefs = userPrefs

        val backtrackPref = switch(R.string.pref_backtrack_enabled)
        backtrackPref?.isEnabled = !(prefs.isLowPowerModeOn && prefs.lowPowerModeDisablesBacktrack)

        backtrackPref?.setOnPreferenceClickListener {
            if (prefs.backtrackEnabled) {
                BacktrackScheduler.start(requireContext())
            } else {
                BacktrackScheduler.stop(requireContext())
            }
            true
        }

        list(R.string.pref_backtrack_frequency)?.setOnPreferenceChangeListener { _, _ ->
            restartBacktrack()
            true
        }

    }

    private fun restartBacktrack() {
        if (prefs.backtrackEnabled) {
            BacktrackScheduler.stop(requireContext())
            BacktrackScheduler.start(requireContext())
        }
    }

}