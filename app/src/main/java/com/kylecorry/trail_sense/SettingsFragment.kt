package com.kylecorry.trail_sense

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import androidx.annotation.ArrayRes
import androidx.preference.*
import com.kylecorry.trail_sense.astronomy.infrastructure.receivers.SunsetAlarmReceiver
import com.kylecorry.trail_sense.calibration.ui.CalibrateAltimeterFragment
import com.kylecorry.trail_sense.calibration.ui.CalibrateBarometerFragment
import com.kylecorry.trail_sense.calibration.ui.CalibrateCompassFragment
import com.kylecorry.trail_sense.calibration.ui.CalibrateGPSFragment
import com.kylecorry.trail_sense.licenses.LicenseFragment
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorChecker
import com.kylecorry.trail_sense.shared.switchToFragment
import com.kylecorry.trail_sense.weather.infrastructure.WeatherAlarmScheduler
import com.kylecorry.trail_sense.weather.infrastructure.WeatherNotificationService
import com.kylecorry.trailsensecore.domain.units.PressureUnits
import com.kylecorry.trailsensecore.infrastructure.system.IntentUtils
import com.kylecorry.trailsensecore.infrastructure.system.NotificationUtils
import com.kylecorry.trailsensecore.infrastructure.system.PackageUtils


class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        val sensorChecker = SensorChecker(requireContext())
        val userPrefs = UserPreferences(requireContext())
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

        preferenceScreen.findPreference<SwitchPreferenceCompat>(getString(R.string.pref_enable_experimental))
            ?.setOnPreferenceClickListener {
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
            ?.setOnPreferenceClickListener { _ ->
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
                switchToFragment(LicenseFragment(), addToBackStack = true)
                false
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

        val forecastSensitivity =
            preferenceScreen.findPreference<ListPreference>(getString(R.string.pref_forecast_sensitivity))
        forecastSensitivity?.setEntries(getForecastSensitivityArray(userPrefs.pressureUnits))

        val stormSensitivity =
            preferenceScreen.findPreference<ListPreference>(getString(R.string.pref_storm_alert_sensitivity))
        stormSensitivity?.setEntries(getStormSensitivityArray(userPrefs.pressureUnits))

        preferenceScreen.findPreference<ListPreference>(getString(R.string.pref_pressure_units))
            ?.setOnPreferenceChangeListener { _, newValue ->
                val mapped = when (newValue) {
                    "hpa" -> PressureUnits.Hpa
                    "mbar" -> PressureUnits.Mbar
                    "in" -> PressureUnits.Inhg
                    else -> PressureUnits.Psi
                }

                forecastSensitivity?.setEntries(getForecastSensitivityArray(mapped))
                stormSensitivity?.setEntries(getStormSensitivityArray(mapped))

                true
            }


        val version = PackageUtils.getVersionName(requireContext())
        preferenceScreen.findPreference<Preference>(getString(R.string.pref_app_version))?.summary =
            version
    }

    @ArrayRes
    private fun getForecastSensitivityArray(units: PressureUnits): Int {
        return when (units) {
            PressureUnits.Hpa -> R.array.forecast_sensitivity_entries_hpa
            PressureUnits.Inhg -> R.array.forecast_sensitivity_entries_in
            PressureUnits.Psi -> R.array.forecast_sensitivity_entries_psi
            else -> R.array.forecast_sensitivity_entries_mbar
        }
    }

    @ArrayRes
    private fun getStormSensitivityArray(units: PressureUnits): Int {
        return when (units) {
            PressureUnits.Hpa -> R.array.storm_sensitivity_entries_hpa
            PressureUnits.Inhg -> R.array.storm_sensitivity_entries_in
            PressureUnits.Psi -> R.array.storm_sensitivity_entries_psi
            else -> R.array.storm_sensitivity_entries_mbar
        }
    }

}