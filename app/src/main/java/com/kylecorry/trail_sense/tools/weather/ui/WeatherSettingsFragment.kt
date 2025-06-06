package com.kylecorry.trail_sense.tools.weather.ui

import android.os.Bundle
import android.view.View
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.sol.math.statistics.Statistics
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.debugging.isDebug
import com.kylecorry.trail_sense.shared.io.IOFactory
import com.kylecorry.trail_sense.shared.permissions.RequestRemoveBatteryRestrictionCommand
import com.kylecorry.trail_sense.shared.preferences.setupNotificationSetting
import com.kylecorry.trail_sense.shared.requireMainActivity
import com.kylecorry.trail_sense.shared.safeRoundToInt
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.observe
import com.kylecorry.trail_sense.tools.weather.WeatherToolRegistration
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherCsvConverter
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherPreferences
import com.kylecorry.trail_sense.tools.weather.infrastructure.alerts.CurrentWeatherAlerter
import com.kylecorry.trail_sense.tools.weather.infrastructure.commands.ChangeWeatherFrequencyCommand
import com.kylecorry.trail_sense.tools.weather.infrastructure.persistence.WeatherRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalTime

class WeatherSettingsFragment : AndromedaPreferenceFragment() {

    private var prefMonitorWeather: SwitchPreferenceCompat? = null
    private var prefWeatherUpdateFrequency: Preference? = null
    private var prefShowDailyWeatherNotification: SwitchPreferenceCompat? = null
    private var prefShowPressureInNotification: SwitchPreferenceCompat? = null
    private var prefShowTemperatureInNotification: SwitchPreferenceCompat? = null
    private var prefleftButton: ListPreference? = null
    private var prefrightButton: ListPreference? = null
    private var prefDailyWeatherTime: Preference? = null
    private var prefStormAlerts: SwitchPreferenceCompat? = null
    private var prefMuteStormAlarmAtNight: SwitchPreferenceCompat? = null
    private val formatService by lazy { FormatService.getInstance(requireContext()) }

    private val weatherMonitorService by lazy {
        Tools.getService(
            requireContext(),
            WeatherToolRegistration.SERVICE_WEATHER_MONITOR
        )!!
    }

    private lateinit var prefs: UserPreferences

    private fun bindPreferences() {
        prefMonitorWeather = switch(R.string.pref_monitor_weather)
        prefWeatherUpdateFrequency = preference(R.string.pref_weather_update_frequency)
        prefShowDailyWeatherNotification = switch(R.string.pref_daily_weather_notification)
        prefShowPressureInNotification = switch(R.string.pref_show_pressure_in_notification)
        prefShowTemperatureInNotification = switch(R.string.pref_show_temperature_in_notification)
        prefStormAlerts = switch(R.string.pref_send_storm_alert)
        prefDailyWeatherTime = preference(R.string.pref_daily_weather_time_holder)
        prefleftButton = list(R.string.pref_weather_quick_action_left)
        prefrightButton = list(R.string.pref_weather_quick_action_right)
        prefMuteStormAlarmAtNight = switch(R.string.pref_weather_mute_storm_alarm_at_night)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.weather_preferences, rootKey)
        val userPrefs = UserPreferences(requireContext())
        prefs = userPrefs
        bindPreferences()

        val actions = Tools.getQuickActions(requireContext())
        val actionNames = actions.map { it.name }
        val actionValues = actions.map { it.id.toString() }

        prefleftButton?.entries = actionNames.toTypedArray()
        prefrightButton?.entries = actionNames.toTypedArray()

        prefleftButton?.entryValues = actionValues.toTypedArray()
        prefrightButton?.entryValues = actionValues.toTypedArray()

        prefMonitorWeather?.isEnabled = !weatherMonitorService.isBlocked()
        prefMonitorWeather?.setOnPreferenceClickListener {
            inBackground {
                if (prefs.weather.shouldMonitorWeather) {
                    weatherMonitorService.enable()
                    RequestRemoveBatteryRestrictionCommand(this@WeatherSettingsFragment).execute()
                } else {
                    weatherMonitorService.disable()
                }
            }
            true
        }
        prefShowDailyWeatherNotification?.setOnPreferenceClickListener {
            restartWeatherMonitor()
            true
        }

        prefWeatherUpdateFrequency?.summary =
            formatService.formatDuration(prefs.weather.weatherUpdateFrequency)
        prefWeatherUpdateFrequency?.setOnPreferenceClickListener {
            ChangeWeatherFrequencyCommand(requireContext()) {
                prefWeatherUpdateFrequency?.summary = formatService.formatDuration(it)
            }.execute()
            true
        }

        prefShowPressureInNotification?.setOnPreferenceClickListener {
            restartWeatherMonitor()
            true
        }

        prefShowTemperatureInNotification?.setOnPreferenceClickListener {
            restartWeatherMonitor()
            true
        }

        prefDailyWeatherTime?.summary =
            formatService.formatTime(prefs.weather.dailyForecastTime, false)
        prefDailyWeatherTime?.setOnPreferenceClickListener {
            Pickers.time(
                requireContext(),
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

        val stormSensitivity =
            preferenceScreen.findPreference<ListPreference>(getString(R.string.pref_storm_alert_sensitivity))
        stormSensitivity?.entries = getStormSensitivities(userPrefs.pressureUnits)

        onClick(preference(R.string.pref_export_weather_csv)) {
            exportWeatherData()
        }

        setupNotificationSetting(
            getString(R.string.pref_weather_monitor_notification_link),
            CurrentWeatherAlerter.WEATHER_CHANNEL_ID,
            getString(R.string.weather_monitor)
        )

        if (isDebug()) {
            val timingPref = preference(R.string.pref_debug_weather_timing)
            timingPref?.isVisible = true

            inBackground {
                onDefault {
                    val readings = WeatherRepo.getInstance(requireContext()).getAll()
                        .zipWithNext { a, b -> Duration.between(a.time, b.time).seconds / 60f }

                    if (readings.isEmpty()) {
                        return@onDefault
                    }

                    val mean = Statistics.mean(readings).safeRoundToInt()
                    val stdev = Statistics.stdev(readings, mean = mean.toFloat()).safeRoundToInt()
                    val max = readings.maxOrNull()?.safeRoundToInt() ?: 0
                    val median = Statistics.median(readings).safeRoundToInt()
                    val quantile75 = Statistics.quantile(readings, 0.75f).safeRoundToInt()
                    val quantile90 = Statistics.quantile(readings, 0.9f).safeRoundToInt()
                    onMain {
                        timingPref?.summary = "Mean: $mean\n" +
                                "Stdev: $stdev\n" +
                                "Max: $max\n" +
                                "Median: $median\n" +
                                "75th: $quantile75\n" +
                                "90th: $quantile90"
                    }
                }
            }

        }

        prefMuteStormAlarmAtNight?.summary = getString(
            R.string.alarm_time_range,
            formatService.formatTime(
                LocalTime.of(8, 0),
                includeSeconds = false,
                includeMinutes = false
            ),
            formatService.formatTime(
                LocalTime.of(20, 0),
                includeSeconds = false,
                includeMinutes = false
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Tools.observe(this, WeatherToolRegistration.BROADCAST_WEATHER_MONITOR_STATE_CHANGED) {
            prefMonitorWeather?.isEnabled = !weatherMonitorService.isBlocked()
            prefMonitorWeather?.isChecked = weatherMonitorService.isEnabled()
            true
        }
    }

    private fun restartWeatherMonitor() {
        inBackground {
            weatherMonitorService.restart()
        }
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
        inBackground(BackgroundMinimumState.Created) {
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