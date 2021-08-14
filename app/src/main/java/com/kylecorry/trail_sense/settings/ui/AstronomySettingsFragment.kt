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
    private var prefLeftQuickAction: ListPreference? = null
    private var prefRightQuickAction: ListPreference? = null


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.astronomy_preferences, rootKey)
        val userPrefs = UserPreferences(requireContext())
        prefs = userPrefs

        list(R.string.pref_sunset_alert_time)?.setOnPreferenceClickListener { _ ->
                context?.apply {
                    sendBroadcast(SunsetAlarmReceiver.intent(this))
                }
                true
            }

        prefLeftQuickAction = list(R.string.pref_astronomy_quick_action_left)
        prefRightQuickAction = list(R.string.pref_astronomy_quick_action_right)

        val actions = QuickActionUtils.astronomy(requireContext())
        val actionNames = actions.map { QuickActionUtils.getName(requireContext(), it) }
        val actionValues = actions.map { it.id.toString() }

        prefLeftQuickAction?.entries = actionNames.toTypedArray()
        prefRightQuickAction?.entries = actionNames.toTypedArray()

        prefLeftQuickAction?.entryValues = actionValues.toTypedArray()
        prefRightQuickAction?.entryValues = actionValues.toTypedArray()


    }

}