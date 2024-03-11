package com.kylecorry.trail_sense.tools.weather.ui

import android.os.Bundle
import android.view.View
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
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
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.andromeda.core.time.Throttle
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.settings.ui.PressureChartPreference
import com.kylecorry.trail_sense.shared.io.IOFactory
import com.kylecorry.trail_sense.shared.permissions.RequestRemoveBatteryRestrictionCommand
import com.kylecorry.trail_sense.shared.preferences.setupNotificationSetting
import com.kylecorry.trail_sense.shared.requireMainActivity
import com.kylecorry.trail_sense.tools.tools.ui.Tools
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherCsvConverter
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherPreferences
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherUpdateScheduler
import com.kylecorry.trail_sense.tools.weather.infrastructure.alerts.CurrentWeatherAlerter
import com.kylecorry.trail_sense.tools.weather.infrastructure.commands.ChangeWeatherFrequencyCommand
import com.kylecorry.trail_sense.tools.weather.infrastructure.persistence.WeatherRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import com.kylecorry.trail_sense.shared.safeRoundToInt
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.weather.domain.RawWeatherObservation
import com.kylecorry.trail_sense.tools.weather.domain.WeatherObservation
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem

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
    private val formatService by lazy { FormatService.getInstance(requireContext()) }

    //barometer
    private val weatherSubsystem by lazy { WeatherSubsystem.getInstance(requireContext()) }
    private var pressureTxt: Preference? = null
    private var seaLevelSwitch: SwitchPreferenceCompat? = null
    private var pressureSmoothingSeekBar: SeekBarPreference? = null
    private var chart: PressureChartPreference? = null
    private var history: List<WeatherObservation> = listOf()
    private var uncalibratedHistory: List<Reading<RawWeatherObservation>> = listOf()
    private lateinit var units: PressureUnits
    private lateinit var sensorService: SensorService
    private val runner = CoroutineQueueRunner()
    private val throttle = Throttle(20)
    private val updateTimer = CoroutineTimer {
        update()
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
        bindPressurePreferences()
    }


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.weather_preferences, rootKey)
        val userPrefs = UserPreferences(requireContext())
        prefs = userPrefs
        sensorService = SensorService(requireContext())
        units = prefs.pressureUnits
        bindPreferences()

        val actions = Tools.getQuickActions(requireContext())
        val actionNames = actions.map { it.name }
        val actionValues = actions.map { it.id.toString() }

        prefleftButton?.entries = actionNames.toTypedArray()
        prefrightButton?.entries = actionNames.toTypedArray()

        prefleftButton?.entryValues = actionValues.toTypedArray()
        prefrightButton?.entryValues = actionValues.toTypedArray()

        prefMonitorWeather?.isEnabled =
            !(prefs.isLowPowerModeOn && prefs.lowPowerModeDisablesWeather)
        prefMonitorWeather?.setOnPreferenceClickListener {
            if (prefs.weather.shouldMonitorWeather) {
                WeatherUpdateScheduler.start(requireContext())
                RequestRemoveBatteryRestrictionCommand(this).execute()
            } else {
                WeatherUpdateScheduler.stop(requireContext())
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

                    if (readings.isEmpty()){
                        return@onDefault
                    }

                    val mean = Statistics.mean(readings).safeRoundToInt()
                    val stdev = Statistics.stdev(readings, mean=mean.toFloat()).safeRoundToInt()
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
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observe(weatherSubsystem.weatherChanged) {
            inBackground {
                runner.replace {
                    history = weatherSubsystem.getHistory()
                    uncalibratedHistory = weatherSubsystem.getRawHistory()
                    onMain {
                        updateChart()
                    }
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        updateTimer.interval(200)
    }

    override fun onPause() {
        super.onPause()
        updateTimer.stop()
    }

    private fun restartWeatherMonitor() {
        WeatherUpdateScheduler.restart(requireContext())
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

    /**
     * Initialize and display pressure settings
     */
    private fun bindPressurePreferences() {
        pressureSmoothingSeekBar = seekBar(R.string.pref_barometer_pressure_smoothing)

        pressureTxt = findPreference(getString(R.string.pref_holder_pressure))
        seaLevelSwitch = findPreference(getString(R.string.pref_use_sea_level_pressure))
        chart = findPreference(getString(R.string.pref_holder_pressure_chart))

        pressureSmoothingSeekBar?.summary =
            formatService.formatPercentage(prefs.weather.pressureSmoothing)

        pressureSmoothingSeekBar?.updatesContinuously = true
        pressureSmoothingSeekBar?.setOnPreferenceChangeListener { _, newValue ->
            val change = 100 * newValue.toString().toFloat() / 1000f
            pressureSmoothingSeekBar?.summary = formatService.formatPercentage(change)
            true
        }

        preference(R.string.pref_barometer_info_holder)?.icon?.setTint(
            Resources.getAndroidColorAttr(
                requireContext(),
                android.R.attr.textColorSecondary
            )
        )

    }

    /**
     * to plot and display pressure readings
     */
    private fun updateChart() {
        val displayReadings = history.filter {
            Duration.between(
                it.time,
                Instant.now()
            ) <= prefs.weather.pressureHistory
        }.map { it.pressureReading() }

        val useTemperature = prefs.weather.seaLevelFactorInTemp
        val useSeaLevel = prefs.weather.useSeaLevelPressure

        val displayRawReadings = uncalibratedHistory.filter {
            Duration.between(
                it.time,
                Instant.now()
            ) <= prefs.weather.pressureHistory
        }.map {
            if (useSeaLevel) {
                Reading(it.value.seaLevel(useTemperature), it.time)
            } else {
                Reading(Pressure.hpa(it.value.pressure), it.time)
            }
        }
        if (displayReadings.isNotEmpty()) {
            chart?.plot(
                displayReadings.map { it.copy(value = it.value.convertTo(units)) },
                displayRawReadings.map { it.copy(value = it.value.convertTo(units)) })
        }
    }


    private fun update() {
        // TODO: Only call this on change
        if (throttle.isThrottled()) {
            return
        }

        val pressure = history.lastOrNull()?.pressure ?: Pressure.hpa(0f)

        pressureTxt?.summary =
            formatService.formatPressure(
                pressure.convertTo(units),
                Units.getDecimalPlaces(units)
            )
    }

}