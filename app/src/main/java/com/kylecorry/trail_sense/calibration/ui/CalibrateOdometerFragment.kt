package com.kylecorry.trail_sense.calibration.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounterService


class CalibrateOdometerFragment : AndromedaPreferenceFragment() {

    private lateinit var strideLengthPref: Preference
    private lateinit var permissionPref: Preference
    private var enabledPref: SwitchPreferenceCompat? = null
    private var showNotificationsPref: SwitchPreferenceCompat? = null
    private val userPrefs by lazy { UserPreferences(requireContext()) }
    private val formatService by lazy { FormatService(requireContext()) }
    private var wasEnabled = false
    private val cache by lazy { Preferences(requireContext()) }


    private val intervalometer = Timer {
        updateStrideLength()
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
        strideLengthPref = findPreference(getString(R.string.pref_stride_length_holder))!!
        permissionPref = findPreference(getString(R.string.pref_odometer_request_permission))!!
        showNotificationsPref = switch(R.string.pref_show_pedometer_notification)

        onClick(enabledPref) {
            if (userPrefs.pedometer.isEnabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    requestPermissions(listOf(Manifest.permission.ACTIVITY_RECOGNITION)) {
                        updatePedometerService()
                    }
                }
            }
        }

        onClick(showNotificationsPref){
            handleNotification()
        }

        permissionPref.setOnPreferenceClickListener {
            val intent = Intents.appSettings(requireContext())
            getResult(intent) { _, _ ->

            }
            true
        }

        strideLengthPref.setOnPreferenceClickListener {
            CustomUiUtils.pickDistance(
                requireContext(),
                listOf(userPrefs.baseDistanceUnits),
                userPrefs.pedometer.strideLength.convertTo(userPrefs.baseDistanceUnits),
                getString(R.string.pref_stride_length_title),
                showFeetAndInches = true
            ) { distance, _ ->
                if (distance != null) {
                    userPrefs.pedometer.strideLength = distance
                    updateStrideLength()
                }
            }
            true
        }
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

    private fun handleNotification(){
        if (!userPrefs.pedometer.showNotification){
            Notify.cancel(requireContext(), StepCounterService.NOTIFICATION_ID)
        }
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

    private fun updateStrideLength() {
        strideLengthPref.summary = formatService.formatDistance(
            userPrefs.pedometer.strideLength.convertTo(userPrefs.baseDistanceUnits),
            2
        )
    }

}