package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import android.text.InputType
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorChecker
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils

class SensorSettingsFragment : CustomPreferenceFragment() {

    private val navigationMap = mapOf(
        R.string.pref_sensor_details to R.id.action_action_settings_to_diagnosticFragment,
        R.string.pref_cell_signal_settings to R.id.action_action_settings_to_cellSignalSettingsFragment,
        R.string.pref_compass_sensor to R.id.action_action_settings_to_calibrateCompassFragment,
        R.string.pref_altimeter_calibration to R.id.action_action_settings_to_calibrateAltimeterFragment,
        R.string.pref_gps_calibration to R.id.action_action_settings_to_calibrateGPSFragment,
        R.string.pref_barometer_calibration to R.id.action_action_settings_to_calibrateBarometerFragment,
        R.string.pref_temperature_settings to R.id.action_action_settings_to_thermometerSettingsFragment,
        R.string.pref_odometer_calibration to R.id.action_action_settings_to_calibrateOdometerFragment,
        R.string.pref_cell_signal_settings to R.id.action_action_settings_to_cellSignalSettingsFragment
    )

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.sensor_preferences, rootKey)

        setIconColor(UiUtils.androidTextColorSecondary(requireContext()))

        for (nav in navigationMap) {
            navigateOnClick(preference(nav.key), nav.value)
        }

        val sensorChecker = SensorChecker(requireContext())
        preference(R.string.pref_barometer_calibration)?.isVisible = sensorChecker.hasBarometer()

        editText(R.string.pref_ruler_calibration)
            ?.setOnBindEditTextListener { editText ->
                editText.inputType =
                    InputType.TYPE_CLASS_NUMBER.or(InputType.TYPE_NUMBER_FLAG_DECIMAL)
            }
    }

}