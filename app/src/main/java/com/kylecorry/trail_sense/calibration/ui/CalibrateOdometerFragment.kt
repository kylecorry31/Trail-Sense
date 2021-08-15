package com.kylecorry.trail_sense.calibration.ui

import android.Manifest
import android.hardware.Sensor
import android.os.Build
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.sense.SensorChecker
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.speedometer.infrastructure.PedometerService


class CalibrateOdometerFragment : AndromedaPreferenceFragment() {

    private lateinit var strideLengthPref: Preference
    private lateinit var permissionPref: Preference
    private lateinit var odometerSourceList: ListPreference
    private val userPrefs by lazy { UserPreferences(requireContext()) }
    private val formatService by lazy { FormatServiceV2(requireContext()) }
    private val sensorChecker by lazy { SensorChecker(requireContext()) }
    private var wasEnabled = false
    private val cache by lazy { Preferences(requireContext()) }


    private val intervalometer = Timer {
        updateStrideLength()
        updatePermissionRequestPreference()
        if (wasEnabled != userPrefs.usePedometer) {
            updatePedometerService()
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.odometer_calibration, rootKey)
        setIconColor(Resources.androidTextColorSecondary(requireContext()))
        bindPreferences()
    }

    private fun bindPreferences() {
        strideLengthPref = findPreference(getString(R.string.pref_stride_length_holder))!!
        odometerSourceList = findPreference(getString(R.string.pref_odometer_source))!!
        permissionPref = findPreference(getString(R.string.pref_odometer_request_permission))!!

        val hasPedometer = sensorChecker.hasSensor(Sensor.TYPE_STEP_COUNTER)
        strideLengthPref.isVisible = hasPedometer
        odometerSourceList.isVisible = hasPedometer
        permissionPref.isVisible = hasPedometer

        permissionPref.setOnPreferenceClickListener {
            val intent = Intents.appSettings(requireContext())
            getResult(intent) { _, _ ->

            }
            true
        }

        odometerSourceList.setOnPreferenceChangeListener { _, newValue ->
            if (newValue == "pedometer") {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    requestPermissions(listOf(Manifest.permission.ACTIVITY_RECOGNITION)) {
                        updatePedometerService()
                    }
                }
            }
            true
        }

        strideLengthPref.setOnPreferenceClickListener {
            CustomUiUtils.pickDistance(
                requireContext(),
                listOf(userPrefs.baseDistanceUnits),
                userPrefs.strideLength.convertTo(userPrefs.baseDistanceUnits),
                getString(R.string.pref_stride_length_title)
            ) {
                if (it != null) {
                    userPrefs.strideLength = it
                    updateStrideLength()
                }
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        wasEnabled = userPrefs.usePedometer
        intervalometer.interval(20)
    }

    override fun onPause() {
        intervalometer.stop()
        super.onPause()
    }

    private fun updatePermissionRequestPreference() {
        val hasActivityRecognition = Permissions.canRecognizeActivity(requireContext())
        permissionPref.isVisible =
            (userPrefs.usePedometer && !hasActivityRecognition) || (!userPrefs.usePedometer && !Permissions.isBackgroundLocationEnabled(
                requireContext()
            ))
    }

    private fun updatePedometerService() {
        if (userPrefs.usePedometer) {
            if (cache.getBoolean("pedometer_battery_sent") != true) {
                Alerts.dialog(
                    requireContext(),
                    getString(R.string.pedometer),
                    getString(R.string.pedometer_disclaimer),
                    cancelText = null
                )
                cache.putBoolean("pedometer_battery_sent", true)
            }
            PedometerService.start(requireContext())
        } else {
            PedometerService.stop(requireContext())
        }

        wasEnabled = userPrefs.usePedometer
    }

    private fun updateStrideLength() {
        strideLengthPref.isEnabled = userPrefs.usePedometer
        strideLengthPref.summary = formatService.formatDistance(
            userPrefs.strideLength.convertTo(userPrefs.baseDistanceUnits),
            2
        )
    }

}