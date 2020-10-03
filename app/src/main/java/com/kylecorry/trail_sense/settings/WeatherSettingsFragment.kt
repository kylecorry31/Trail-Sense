package com.kylecorry.trail_sense.settings

import android.os.Bundle
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.preference.*
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.weather.infrastructure.WeatherUpdateScheduler
import com.kylecorry.trailsensecore.domain.units.PressureUnits

class WeatherSettingsFragment : PreferenceFragmentCompat() {

    private var prefMonitorWeather: SwitchPreferenceCompat? = null
    private var prefWeatherUpdateFrequency: ListPreference? = null
    private var prefForceWeatherUpdates: SwitchPreferenceCompat? = null
    private var prefShowWeatherNotification: SwitchPreferenceCompat? = null
    private var prefShowPressureInNotification: SwitchPreferenceCompat? = null
    private var prefStormAlerts: SwitchPreferenceCompat? = null

    private lateinit var prefs: UserPreferences

    private fun bindPreferences() {
        prefMonitorWeather = switch(R.string.pref_monitor_weather)
        prefWeatherUpdateFrequency = list(R.string.pref_weather_update_frequency)
        prefForceWeatherUpdates = switch(R.string.pref_force_weather_updates)
        prefShowWeatherNotification = switch(R.string.pref_show_weather_notification)
        prefShowPressureInNotification = switch(R.string.pref_show_pressure_in_notification)
        prefStormAlerts = switch(R.string.pref_send_storm_alert)
    }


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.weather_preferences, rootKey)
        val userPrefs = UserPreferences(requireContext())
        prefs = userPrefs
        bindPreferences()

        prefMonitorWeather?.setOnPreferenceClickListener {
            if (prefs.weather.shouldMonitorWeather) {
                WeatherUpdateScheduler.start(requireContext())
            } else {
                WeatherUpdateScheduler.stop(requireContext())
            }
            true
        }
        prefForceWeatherUpdates?.setOnPreferenceClickListener {
            restartWeatherMonitor()
            true
        }
        prefShowWeatherNotification?.setOnPreferenceClickListener {
            restartWeatherMonitor()
            true
        }
        prefWeatherUpdateFrequency?.setOnPreferenceChangeListener { _, _ ->
            restartWeatherMonitor()
            true
        }
        prefShowPressureInNotification?.setOnPreferenceClickListener {
            restartWeatherMonitor()
            true
        }

        val forecastSensitivity =
            preferenceScreen.findPreference<ListPreference>(getString(R.string.pref_forecast_sensitivity))
        forecastSensitivity?.setEntries(getForecastSensitivityArray(userPrefs.pressureUnits))

        val stormSensitivity =
            preferenceScreen.findPreference<ListPreference>(getString(R.string.pref_storm_alert_sensitivity))
        stormSensitivity?.setEntries(getStormSensitivityArray(userPrefs.pressureUnits))
    }

    private fun restartWeatherMonitor() {
        WeatherUpdateScheduler.stop(requireContext())
        WeatherUpdateScheduler.start(requireContext())
    }

    private fun switch(@StringRes id: Int): SwitchPreferenceCompat? {
        return preferenceManager.findPreference(getString(id))
    }

    private fun list(@StringRes id: Int): ListPreference? {
        return preferenceManager.findPreference(getString(id))
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