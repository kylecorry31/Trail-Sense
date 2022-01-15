package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.io.IOFactory
import com.kylecorry.trail_sense.weather.infrastructure.WeatherContextualService
import com.kylecorry.trail_sense.weather.infrastructure.WeatherCsvConverter
import com.kylecorry.trail_sense.weather.infrastructure.WeatherPreferences
import com.kylecorry.trail_sense.weather.infrastructure.WeatherUpdateScheduler
import com.kylecorry.trail_sense.weather.infrastructure.persistence.WeatherRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

class WeatherSettingsFragment : AndromedaPreferenceFragment() {

    private var prefMonitorWeather: SwitchPreferenceCompat? = null
    private var prefWeatherUpdateFrequency: Preference? = null
    private var prefShowWeatherNotification: SwitchPreferenceCompat? = null
    private var prefShowDailyWeatherNotification: SwitchPreferenceCompat? = null
    private var prefShowPressureInNotification: SwitchPreferenceCompat? = null
    private var prefLeftQuickAction: ListPreference? = null
    private var prefRightQuickAction: ListPreference? = null
    private var prefDailyWeatherTime: Preference? = null
    private var prefStormAlerts: SwitchPreferenceCompat? = null
    private val formatService by lazy { FormatService(requireContext()) }

    private lateinit var prefs: UserPreferences

    private fun bindPreferences() {
        prefMonitorWeather = switch(R.string.pref_monitor_weather)
        prefWeatherUpdateFrequency = preference(R.string.pref_weather_update_frequency)
        prefShowWeatherNotification = switch(R.string.pref_show_weather_notification)
        prefShowDailyWeatherNotification = switch(R.string.pref_daily_weather_notification)
        prefShowPressureInNotification = switch(R.string.pref_show_pressure_in_notification)
        prefStormAlerts = switch(R.string.pref_send_storm_alert)
        prefDailyWeatherTime = preference(R.string.pref_daily_weather_time_holder)
        prefLeftQuickAction = list(R.string.pref_weather_quick_action_left)
        prefRightQuickAction = list(R.string.pref_weather_quick_action_right)
    }


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.weather_preferences, rootKey)
        val userPrefs = UserPreferences(requireContext())
        prefs = userPrefs
        bindPreferences()

        val actions = QuickActionUtils.weather(requireContext())
        val actionNames = actions.map { QuickActionUtils.getName(requireContext(), it) }
        val actionValues = actions.map { it.id.toString() }

        prefLeftQuickAction?.entries = actionNames.toTypedArray()
        prefRightQuickAction?.entries = actionNames.toTypedArray()

        prefLeftQuickAction?.entryValues = actionValues.toTypedArray()
        prefRightQuickAction?.entryValues = actionValues.toTypedArray()

        prefMonitorWeather?.isEnabled =
            !(prefs.isLowPowerModeOn && prefs.lowPowerModeDisablesWeather)
        prefMonitorWeather?.setOnPreferenceClickListener {
            if (prefs.weather.shouldMonitorWeather) {
                WeatherUpdateScheduler.start(requireContext())
            } else {
                WeatherUpdateScheduler.stop(requireContext())
            }
            true
        }
        prefShowWeatherNotification?.setOnPreferenceClickListener {
            restartWeatherMonitor()
            true
        }
        prefShowDailyWeatherNotification?.setOnPreferenceClickListener {
            restartWeatherMonitor()
            true
        }

        prefWeatherUpdateFrequency?.summary =
            formatService.formatDuration(prefs.weather.weatherUpdateFrequency)
        prefWeatherUpdateFrequency?.setOnPreferenceClickListener {
            val title = it.title.toString()
            CustomUiUtils.pickDuration(
                requireContext(),
                prefs.weather.weatherUpdateFrequency,
                title,
                getString(R.string.actual_frequency_disclaimer)
            ) {
                if (it != null && !it.isZero) {
                    prefs.weather.weatherUpdateFrequency = it
                    prefWeatherUpdateFrequency?.summary = formatService.formatDuration(it)
                    restartWeatherMonitor()
                }
            }
            true
        }
        prefShowPressureInNotification?.setOnPreferenceClickListener {
            restartWeatherMonitor()
            true
        }

        prefDailyWeatherTime?.summary =
            formatService.formatTime(prefs.weather.dailyForecastTime, false)
        prefDailyWeatherTime?.setOnPreferenceClickListener {
            CustomUiUtils.pickTime(
                this,
                prefs.use24HourTime,
                prefs.weather.dailyForecastTime
            ) { time ->
                if (time != null) {
                    prefs.weather.dailyForecastTime = time
                    it.summary = formatService.formatTime(time, false)
                    restartWeatherMonitor()
                }
            }
            true
        }

        val forecastSensitivity =
            preferenceScreen.findPreference<ListPreference>(getString(R.string.pref_forecast_sensitivity))
        forecastSensitivity?.entries = getForecastSensitivities(userPrefs.pressureUnits)

        forecastSensitivity?.setOnPreferenceChangeListener { _, _ ->
            lifecycleScope.launch {
                WeatherContextualService.getInstance(requireContext()).setDataChanged()
            }
            true
        }

        val stormSensitivity =
            preferenceScreen.findPreference<ListPreference>(getString(R.string.pref_storm_alert_sensitivity))
        stormSensitivity?.entries = getStormSensitivities(userPrefs.pressureUnits)

        stormSensitivity?.setOnPreferenceChangeListener { _, _ ->
            lifecycleScope.launch {
                WeatherContextualService.getInstance(requireContext()).setDataChanged()
            }
            true
        }

        onClick(preference(R.string.pref_export_weather_csv)) {
            exportWeatherData()
        }
    }

    private fun restartWeatherMonitor() {
        WeatherUpdateScheduler.stop(requireContext())
        WeatherUpdateScheduler.start(requireContext())
    }

    private fun getForecastSensitivities(units: PressureUnits): Array<CharSequence> {
        val hpa = listOf(
            Pressure(WeatherPreferences.HPA_FORECAST_LOW, PressureUnits.Hpa),
            Pressure(WeatherPreferences.HPA_FORECAST_MEDIUM, PressureUnits.Hpa),
            Pressure(WeatherPreferences.HPA_FORECAST_HIGH, PressureUnits.Hpa),
        )

        val stringValues = hpa.map {
            getString(
                R.string.pressure_tendency_format_2, formatService.formatPressure(
                    it.convertTo(units),
                    Units.getDecimalPlaces(units) + 1,
                    false
                )
            )
        }

        return arrayOf(
            getString(R.string.low_amount, stringValues[0]),
            getString(R.string.medium_amount, stringValues[1]),
            getString(R.string.high_amount, stringValues[2])
        )
    }

    private fun getStormSensitivities(units: PressureUnits): Array<CharSequence> {
        val hpa = listOf(
            Pressure(WeatherPreferences.HPA_STORM_LOW, PressureUnits.Hpa),
            Pressure(WeatherPreferences.HPA_STORM_MEDIUM, PressureUnits.Hpa),
            Pressure(WeatherPreferences.HPA_STORM_HIGH, PressureUnits.Hpa),
        )

        val stringValues = hpa.map {
            getString(
                R.string.pressure_tendency_format_2, formatService.formatPressure(
                    it.convertTo(units),
                    Units.getDecimalPlaces(units) + 1,
                    false
                )
            )
        }

        return arrayOf(
            getString(R.string.low_amount, stringValues[0]),
            getString(R.string.medium_amount, stringValues[1]),
            getString(R.string.high_amount, stringValues[2])
        )
    }

    private fun exportWeatherData() {
        val exporter = IOFactory().createCsvService(requireMainActivity())
        val repo = WeatherRepo.getInstance(requireContext())
        lifecycleScope.launch {
            val exported = withContext(Dispatchers.IO) {
                val readings = repo.getAll().sortedByDescending { it.time }
                val csv = WeatherCsvConverter().toCSV(readings)
                exporter.export(csv, "weather-${Instant.now().toEpochMilli()}.csv")
            }
            if (exported) {
                withContext(Dispatchers.Main) {
                    Alerts.toast(requireContext(), getString(R.string.weather_exported))
                }
            }
        }
    }

}