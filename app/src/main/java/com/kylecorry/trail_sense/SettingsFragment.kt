package com.kylecorry.trail_sense

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import androidx.core.app.ShareCompat
import androidx.preference.*
import com.kylecorry.trail_sense.astronomy.infrastructure.receivers.SunsetAlarmReceiver
import com.kylecorry.trail_sense.calibration.ui.CalibrateAltimeterFragment
import com.kylecorry.trail_sense.calibration.ui.CalibrateBarometerFragment
import com.kylecorry.trail_sense.calibration.ui.CalibrateCompassFragment
import com.kylecorry.trail_sense.calibration.ui.CalibrateGPSFragment
import com.kylecorry.trail_sense.shared.sensors.SensorChecker
import com.kylecorry.trail_sense.shared.switchToFragment
import com.kylecorry.trail_sense.shared.system.IntentUtils
import com.kylecorry.trail_sense.shared.system.NotificationUtils
import com.kylecorry.trail_sense.shared.system.PackageUtils
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

        preferenceScreen.findPreference<Preference>(getString(R.string.pref_compass_sensor))
            ?.setOnPreferenceClickListener { _ ->
                // Launch intent
                switchToFragment(CalibrateCompassFragment(), addToBackStack = true)
                false
            }

        preferenceScreen.findPreference<Preference>(getString(R.string.pref_altimeter_calibration))
            ?.setOnPreferenceClickListener { _ ->
                // Launch intent
                switchToFragment(CalibrateAltimeterFragment(), addToBackStack = true)
                false
            }

        preferenceScreen.findPreference<Preference>(getString(R.string.pref_gps_calibration))
            ?.setOnPreferenceClickListener { _ ->
                // Launch intent
                switchToFragment(CalibrateGPSFragment(), addToBackStack = true)
                false
            }

        preferenceScreen.findPreference<Preference>(getString(R.string.pref_barometer_calibration))
            ?.setOnPreferenceClickListener { _ ->
                // Launch intent
                switchToFragment(CalibrateBarometerFragment(), addToBackStack = true)
                false
            }

        preferenceScreen.findPreference<ListPreference>(getString(R.string.pref_theme))
            ?.setOnPreferenceChangeListener { _, _ ->
                activity?.recreate()
                true
            }

        preferenceScreen.findPreference<SwitchPreferenceCompat>(getString(R.string.pref_monitor_weather))
            ?.setOnPreferenceChangeListener { _, value ->
                val shouldMonitorWeather = value as Boolean
                context?.apply {
                    if (shouldMonitorWeather) {
                        WeatherAlarmScheduler.start(this)
                    } else {
                        WeatherAlarmScheduler.stop(this)
                    }
                }

                true
            }

        preferenceScreen.findPreference<SwitchPreferenceCompat>(getString(R.string.pref_show_weather_notification))
            ?.setOnPreferenceChangeListener { _, value ->
                val shouldShowWeatherNotification = value as Boolean
                context?.apply {
                    if (shouldShowWeatherNotification) {
                        WeatherAlarmScheduler.start(this)
                    } else {
                        NotificationUtils.cancel(
                            this,
                            WeatherNotificationService.WEATHER_NOTIFICATION_ID
                        )
                    }
                }

                true
            }

        preferenceScreen.findPreference<SwitchPreferenceCompat>(getString(R.string.pref_show_pressure_in_notification))
            ?.setOnPreferenceClickListener {
                context?.apply {
                    NotificationUtils.cancel(
                        this,
                        WeatherNotificationService.WEATHER_NOTIFICATION_ID
                    )
                    WeatherAlarmScheduler.start(this)
                }

                true
            }

        preferenceScreen.findPreference<ListPreference>(getString(R.string.pref_sunset_alert_time))
            ?.setOnPreferenceChangeListener { _, value ->
                context?.apply {
                    sendBroadcast(SunsetAlarmReceiver.intent(this))
                }
                true
            }

        preferenceScreen.findPreference<EditTextPreference>(getString(R.string.pref_ruler_calibration))
            ?.setOnBindEditTextListener { editText ->
                editText.inputType =
                    InputType.TYPE_CLASS_NUMBER.or(InputType.TYPE_NUMBER_FLAG_DECIMAL)
            }

        preferenceScreen.findPreference<EditTextPreference>(getString(R.string.pref_num_visible_beacons))
            ?.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER
            }

        preferenceScreen.findPreference<Preference>(getString(R.string.pref_open_source_licenses))
            ?.setOnPreferenceClickListener {
                UiUtils.alert(
                    requireContext(),
                    getString(R.string.pref_open_source_licenses_title),
                    getString(
                        R.string.licenses
                    )
                )
                true
            }

        preferenceScreen.findPreference<Preference>(getString(R.string.pref_github))
            ?.setOnPreferenceClickListener {
                val i = IntentUtils.url(it.summary.toString())
                startActivity(i)
                true
            }

        preferenceScreen.findPreference<Preference>(getString(R.string.pref_email))
            ?.setOnPreferenceClickListener {

                val intent = IntentUtils.email(it.summary.toString(), getString(R.string.app_name))
                startActivity(Intent.createChooser(intent, it.title.toString()))
                true
            }

        val version = PackageUtils.getVersionName(requireContext())
        preferenceScreen.findPreference<Preference>(getString(R.string.pref_app_version))?.summary =
            version
    }
}