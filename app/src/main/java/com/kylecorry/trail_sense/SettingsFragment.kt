package com.kylecorry.trail_sense

import android.os.Bundle
import android.text.InputType
import androidx.preference.*
import com.kylecorry.trail_sense.astronomy.infrastructure.receivers.SunsetAlarmReceiver
import com.kylecorry.trail_sense.calibration.ui.CalibrateAltimeterFragment
import com.kylecorry.trail_sense.calibration.ui.CalibrateBarometerFragment
import com.kylecorry.trail_sense.calibration.ui.CalibrateCompassFragment
import com.kylecorry.trail_sense.calibration.ui.CalibrateGPSFragment
import com.kylecorry.trail_sense.shared.sensors.SensorChecker
import com.kylecorry.trail_sense.shared.switchToFragment
import com.kylecorry.trail_sense.shared.system.NotificationUtils
import com.kylecorry.trail_sense.shared.system.UiUtils
import com.kylecorry.trail_sense.weather.infrastructure.WeatherAlarmScheduler
import com.kylecorry.trail_sense.weather.infrastructure.WeatherNotificationService


class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        // TODO: List open source licenses
        // Austin Andrews - weather icons
        // Michael Irigoyen - moon icons
        val sensorChecker = SensorChecker(requireContext())
        if (!sensorChecker.hasBarometer()) {
            preferenceScreen.removePreferenceRecursively(getString(R.string.pref_weather_category))
            preferenceScreen.removePreferenceRecursively(getString(R.string.pref_barometer_calibration))
        }

        preferenceScreen.findPreference<Preference>(getString(R.string.pref_compass_sensor))?.setOnPreferenceClickListener { _ ->
            // Launch intent
            switchToFragment(CalibrateCompassFragment(), addToBackStack = true)
            false
        }

        preferenceScreen.findPreference<Preference>(getString(R.string.pref_altimeter_calibration))?.setOnPreferenceClickListener { _ ->
            // Launch intent
            switchToFragment(CalibrateAltimeterFragment(), addToBackStack = true)
            false
        }

        preferenceScreen.findPreference<Preference>(getString(R.string.pref_gps_calibration))?.setOnPreferenceClickListener { _ ->
            // Launch intent
            switchToFragment(CalibrateGPSFragment(), addToBackStack = true)
            false
        }

        preferenceScreen.findPreference<Preference>(getString(R.string.pref_barometer_calibration))?.setOnPreferenceClickListener { _ ->
            // Launch intent
            switchToFragment(CalibrateBarometerFragment(), addToBackStack = true)
            false
        }

        preferenceScreen.findPreference<ListPreference>(getString(R.string.pref_theme))?.setOnPreferenceChangeListener { _, _ ->
            activity?.recreate()
            true
        }

        preferenceScreen.findPreference<SwitchPreferenceCompat>(getString(R.string.pref_monitor_weather))?.setOnPreferenceChangeListener { _, value ->
            val shouldMonitorWeather = value as Boolean
            context?.apply {
                if (shouldMonitorWeather){
                    WeatherAlarmScheduler.start(this)
                } else {
                    WeatherAlarmScheduler.stop(this)
                }
            }

            true
        }

        preferenceScreen.findPreference<SwitchPreferenceCompat>(getString(R.string.pref_show_weather_notification))?.setOnPreferenceChangeListener { _, value ->
            val shouldShowWeatherNotification = value as Boolean
            context?.apply {
                if (shouldShowWeatherNotification){
                    WeatherAlarmScheduler.start(this)
                } else {
                    NotificationUtils.cancel(this, WeatherNotificationService.WEATHER_NOTIFICATION_ID)
                }
            }

            true
        }

        preferenceScreen.findPreference<ListPreference>(getString(R.string.pref_sunset_alert_time))?.setOnPreferenceChangeListener { _, value ->
            context?.apply {
                sendBroadcast(SunsetAlarmReceiver.intent(this))
            }
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