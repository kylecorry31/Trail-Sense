package com.kylecorry.trail_sense.tools.astronomy.ui

import android.os.Bundle
import android.view.View
import androidx.preference.ListPreference
import androidx.preference.SwitchPreferenceCompat
import com.kylecorry.andromeda.core.topics.generic.asLiveData
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.tools.astronomy.AstronomyToolRegistration
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.receivers.SunriseAlarmReceiver
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.receivers.SunsetAlarmReceiver
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import java.time.Duration

class AstronomySettingsFragment : AndromedaPreferenceFragment() {

    private lateinit var prefs: UserPreferences
    private var prefleftButton: ListPreference? = null
    private var prefrightButton: ListPreference? = null
    private var prefSunsetAlertsSwitch: SwitchPreferenceCompat? = null
    private var prefSunriseAlertsSwitch: SwitchPreferenceCompat? = null
    private val sunsetService by lazy {
        Tools.getService(
            requireContext(),
            AstronomyToolRegistration.SERVICE_SUNSET_ALERTS
        )!!
    }
    private val sunriseService by lazy {
        Tools.getService(
            requireContext(),
            AstronomyToolRegistration.SERVICE_SUNRISE_ALERTS
        )!!
    }
    private val formatter by lazy { FormatService.getInstance(requireContext()) }

    override fun onResume() {
        super.onResume()
        Tools.subscribe(
            AstronomyToolRegistration.BROADCAST_SUNSET_ALERTS_ENABLED,
            ::onSunsetAlertsChanged
        )
        Tools.subscribe(
            AstronomyToolRegistration.BROADCAST_SUNSET_ALERTS_DISABLED,
            ::onSunsetAlertsChanged
        )
        Tools.subscribe(
            AstronomyToolRegistration.BROADCAST_SUNRISE_ALERTS_ENABLED,
            ::onSunriseAlertsChanged
        )
        Tools.subscribe(
            AstronomyToolRegistration.BROADCAST_SUNRISE_ALERTS_DISABLED,
            ::onSunriseAlertsChanged
        )
    }

    override fun onPause() {
        super.onPause()
        Tools.unsubscribe(
            AstronomyToolRegistration.BROADCAST_SUNSET_ALERTS_ENABLED,
            ::onSunsetAlertsChanged
        )
        Tools.unsubscribe(
            AstronomyToolRegistration.BROADCAST_SUNSET_ALERTS_DISABLED,
            ::onSunsetAlertsChanged
        )
        Tools.unsubscribe(
            AstronomyToolRegistration.BROADCAST_SUNRISE_ALERTS_ENABLED,
            ::onSunriseAlertsChanged
        )
        Tools.unsubscribe(
            AstronomyToolRegistration.BROADCAST_SUNRISE_ALERTS_DISABLED,
            ::onSunriseAlertsChanged
        )
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.astronomy_preferences, rootKey)
        val userPrefs = UserPreferences(requireContext())
        prefs = userPrefs

        prefleftButton = list(R.string.pref_astronomy_quick_action_left)
        prefrightButton = list(R.string.pref_astronomy_quick_action_right)

        prefSunsetAlertsSwitch = switch(R.string.pref_sunset_alerts)
        prefSunriseAlertsSwitch = switch(R.string.pref_sunrise_alerts)

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
                    sunsetService.disable()
                }
            }
        }

        onClick(prefSunriseAlertsSwitch) {
            inBackground {
                if (prefs.astronomy.sendSunriseAlerts) {
                    SunriseAlarmReceiver.enable(this@AstronomySettingsFragment, true)
                } else {
                    sunriseService.disable()
                }
            }
        }

        list(R.string.pref_sunrise_alert_time)?.apply {
            setTimeEntries(
                this, listOf(
                    0L,
                    15L,
                    30L,
                    45L,
                    60L,
                    90L,
                    120L
                )
            )
        }

        list(R.string.pref_sunset_alert_time)?.apply {
            setTimeEntries(
                this, listOf(
                    30L,
                    60L,
                    90L,
                    120L,
                    150L,
                    180L
                )
            )
        }

    }

    private fun setTimeEntries(pref: ListPreference, minutes: List<Long>) {
        val entries = minutes.map { formatter.formatDuration(Duration.ofMinutes(it)) }
            .toTypedArray()
        val values = minutes.map { it.toString() }.toTypedArray()
        pref.entries = entries
        pref.entryValues = values
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sunsetAlertTimeKey = getString(R.string.pref_sunset_alert_time)
        val sunriseAlertTimeKey = getString(R.string.pref_sunrise_alert_time)

        PreferencesSubsystem.getInstance(requireContext()).preferences.onChange.asLiveData()
            .observe(viewLifecycleOwner) {
                if (it == sunsetAlertTimeKey) {
                    inBackground {
                        sunsetService.restart()
                    }
                } else if (it == sunriseAlertTimeKey) {
                    inBackground {
                        sunriseService.restart()
                    }
                }
            }
    }

    private fun onSunsetAlertsChanged(data: Bundle): Boolean {
        prefSunsetAlertsSwitch?.isChecked = prefs.astronomy.sendSunsetAlerts
        return true
    }

    private fun onSunriseAlertsChanged(data: Bundle): Boolean {
        prefSunriseAlertsSwitch?.isChecked = prefs.astronomy.sendSunriseAlerts
        return true
    }
}