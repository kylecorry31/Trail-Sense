package com.kylecorry.trail_sense.tools.astronomy.ui

import android.os.Bundle
import android.view.View
import androidx.preference.ListPreference
import androidx.preference.SwitchPreferenceCompat
import com.kylecorry.andromeda.core.topics.generic.asLiveData
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.tools.astronomy.AstronomyToolRegistration
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.receivers.SunsetAlarmReceiver
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class AstronomySettingsFragment : AndromedaPreferenceFragment() {

    private lateinit var prefs: UserPreferences
    private var prefleftButton: ListPreference? = null
    private var prefrightButton: ListPreference? = null
    private var prefSunsetAlertsSwitch: SwitchPreferenceCompat? = null
    private val service by lazy {
        Tools.getService(
            requireContext(),
            AstronomyToolRegistration.SERVICE_SUNSET_ALERTS
        )!!
    }

    override fun onResume() {
        super.onResume()
        Tools.subscribe(
            AstronomyToolRegistration.BROADCAST_SUNSET_ALERTS_ENABLED,
            ::onSunsetAlertsEnabled
        )
        Tools.subscribe(
            AstronomyToolRegistration.BROADCAST_SUNSET_ALERTS_DISABLED,
            ::onSunsetAlertsDisabled
        )
    }

    override fun onPause() {
        super.onPause()
        Tools.unsubscribe(
            AstronomyToolRegistration.BROADCAST_SUNSET_ALERTS_ENABLED,
            ::onSunsetAlertsEnabled
        )
        Tools.unsubscribe(
            AstronomyToolRegistration.BROADCAST_SUNSET_ALERTS_DISABLED,
            ::onSunsetAlertsDisabled
        )
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.astronomy_preferences, rootKey)
        val userPrefs = UserPreferences(requireContext())
        prefs = userPrefs

        prefleftButton = list(R.string.pref_astronomy_quick_action_left)
        prefrightButton = list(R.string.pref_astronomy_quick_action_right)

        prefSunsetAlertsSwitch = switch(R.string.pref_sunset_alerts)

        val actions = Tools.getQuickActions(requireContext())
        val actionNames = actions.map { it.name }
        val actionValues = actions.map { it.id.toString() }

        prefleftButton?.entries = actionNames.toTypedArray()
        prefrightButton?.entries = actionNames.toTypedArray()

        prefleftButton?.entryValues = actionValues.toTypedArray()
        prefrightButton?.entryValues = actionValues.toTypedArray()

        switch(R.string.pref_start_camera_in_3d_view)?.isVisible =
            Tools.isToolAvailable(requireContext(), Tools.AUGMENTED_REALITY)

        onClick(prefSunsetAlertsSwitch) {
            inBackground {
                if (prefs.astronomy.sendSunsetAlerts) {
                    SunsetAlarmReceiver.enable(this@AstronomySettingsFragment, true)
                } else {
                    service.disable()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val alertTimePrefKey = getString(R.string.pref_sunset_alert_time)

        PreferencesSubsystem.getInstance(requireContext()).preferences.onChange.asLiveData()
            .observe(viewLifecycleOwner) {
                if (it == alertTimePrefKey) {
                    inBackground {
                        service.restart()
                    }
                }
            }
    }

    private fun onSunsetAlertsEnabled(data: Bundle): Boolean {
        prefSunsetAlertsSwitch?.isChecked = true
        return true
    }

    private fun onSunsetAlertsDisabled(data: Bundle): Boolean {
        prefSunsetAlertsSwitch?.isChecked = false
        return true
    }
}