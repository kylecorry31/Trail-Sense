package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import androidx.preference.ListPreference
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.infrastructure.receivers.SunsetAlarmReceiver
import com.kylecorry.trail_sense.shared.QuickActionUtils
import com.kylecorry.trail_sense.shared.UserPreferences

class AstronomySettingsFragment : AndromedaPreferenceFragment() {

    private lateinit var prefs: UserPreferences
    private var prefleftButton: ListPreference? = null
    private var prefrightButton: ListPreference? = null


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.astronomy_preferences, rootKey)
        val userPrefs = UserPreferences(requireContext())
        prefs = userPrefs

        list(R.string.pref_sunset_alert_time)?.setOnPreferenceClickListener {
            context?.apply {
                    SunsetAlarmReceiver.start(this)
                }
                true
            }

        prefleftButton = list(R.string.pref_astronomy_quick_action_left)
        prefrightButton = list(R.string.pref_astronomy_quick_action_right)

        val actions = QuickActionUtils.astronomy(requireContext())
        val actionNames = actions.map { QuickActionUtils.getName(requireContext(), it) }
        val actionValues = actions.map { it.id.toString() }

        prefleftButton?.entries = actionNames.toTypedArray()
        prefrightButton?.entries = actionNames.toTypedArray()

        prefleftButton?.entryValues = actionValues.toTypedArray()
        prefrightButton?.entryValues = actionValues.toTypedArray()


    }

}