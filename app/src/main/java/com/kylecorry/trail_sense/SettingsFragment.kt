package com.kylecorry.trail_sense

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.kylecorry.trail_sense.shared.sensors.SensorChecker
import com.kylecorry.trail_sense.weather.infrastructure.BarometerService


class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        // TODO: List open source licenses
        // Austin Andrews - weather icons
        // Michael Irigoyen - moon icons
        val sensorChecker = SensorChecker(requireContext())
        if (!sensorChecker.hasBarometer()) {
            preferenceScreen.removePreferenceRecursively(getString(R.string.pref_weather_category))
        }

        preferenceScreen.findPreference<ListPreference>(getString(R.string.pref_theme))?.setOnPreferenceChangeListener { _, _ ->
            activity?.recreate()
            true
        }

        preferenceScreen.findPreference<SwitchPreferenceCompat>(getString(R.string.pref_monitor_weather))?.setOnPreferenceChangeListener { _, value ->
            val shouldMonitorWeather = value as Boolean
            context?.apply {
                if (shouldMonitorWeather){
                    BarometerService.start(this)
                } else {
                    BarometerService.stop(this)
                }
            }

            activity?.recreate()
            true
        }

       preferenceScreen.findPreference<EditTextPreference>(getString(R.string.pref_ruler_calibration))?.setOnBindEditTextListener { editText ->
           editText.inputType = InputType.TYPE_CLASS_NUMBER.or(InputType.TYPE_NUMBER_FLAG_DECIMAL)
       }

        preferenceScreen.findPreference<EditTextPreference>(getString(R.string.pref_num_visible_beacons))?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }
    }
}