package com.kylecorry.trail_sense.tools.pedometer.ui

import android.os.Bundle
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.permissions.alertNoActivityRecognitionPermission
import com.kylecorry.trail_sense.shared.permissions.requestActivityRecognition
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.preferences.setupDistanceSetting
import com.kylecorry.trail_sense.shared.preferences.setupNotificationSetting
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounterService


class PedometerSettingsFragment : AndromedaPreferenceFragment() {

    private lateinit var permissionPref: Preference
    private var enabledPref: SwitchPreferenceCompat? = null
    private val userPrefs by lazy { UserPreferences(requireContext()) }
    private var wasEnabled = false
    private val cache by lazy { PreferencesSubsystem.getInstance(requireContext()).preferences }


    private val intervalometer = CoroutineTimer {
        updatePermissionRequestPreference()
        if (wasEnabled != userPrefs.pedometer.isEnabled) {
            updatePedometerService()
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.odometer_calibration, rootKey)
        setIconColor(Resources.androidTextColorSecondary(requireContext()))
        bindPreferences()
    }

    private fun bindPreferences() {
        enabledPref = switch(R.string.pref_pedometer_enabled)
        permissionPref = findPreference(getString(R.string.pref_odometer_request_permission))!!

        onClick(enabledPref) {
            if (userPrefs.pedometer.isEnabled) {
                requestActivityRecognition { hasPermission ->
                    updatePedometerService()
                    if (!hasPermission) {
                        alertNoActivityRecognitionPermission()
                    }
                }
            }
        }

        permissionPref.setOnPreferenceClickListener {
            val intent = Intents.appSettings(requireContext())
            getResult(intent) { _, _ ->

            }
            true
        }

        setupDistanceSetting(
            getString(R.string.pref_stride_length_holder),
            { userPrefs.pedometer.strideLength.convertTo(userPrefs.baseDistanceUnits) },
            { distance ->
                if (distance != null && distance.distance > 0f) {
                    userPrefs.pedometer.strideLength = distance
                }
            },
            DistanceUtils.humanDistanceUnits,
            showFeetAndInches = true,
            decimalPlacesOverride = 2
        )

        onClick(preference(R.string.pref_estimate_stride_length_holder)) {
            findNavController().navigate(R.id.action_calibrate_pedometer_to_estimate_stride_length)
        }

        setupNotificationSetting(
            getString(R.string.pref_pedometer_notification_link),
            StepCounterService.CHANNEL_ID,
            getString(R.string.pedometer)
        )
    }

    override fun onResume() {
        super.onResume()
        wasEnabled = userPrefs.pedometer.isEnabled
        intervalometer.interval(20)
    }

    override fun onPause() {
        intervalometer.stop()
        super.onPause()
    }

    private fun updatePermissionRequestPreference() {
        val hasActivityRecognition = Permissions.canRecognizeActivity(requireContext())
        permissionPref.isVisible =
            (userPrefs.pedometer.isEnabled && !hasActivityRecognition) || (!userPrefs.pedometer.isEnabled && !Permissions.isBackgroundLocationEnabled(
                requireContext()
            ))
    }

    private fun updatePedometerService() {
        if (userPrefs.pedometer.isEnabled) {
            if (cache.getBoolean("pedometer_battery_sent") != true) {
                Alerts.dialog(
                    requireContext(),
                    getString(R.string.pedometer),
                    getString(R.string.pedometer_disclaimer),
                    cancelText = null
                )
                cache.putBoolean("pedometer_battery_sent", true)
            }
            StepCounterService.start(requireContext())
        } else {
            StepCounterService.stop(requireContext())
        }

        wasEnabled = userPrefs.pedometer.isEnabled
    }

}