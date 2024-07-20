package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.andromeda.core.time.Throttle
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.weather.domain.RawWeatherObservation
import com.kylecorry.trail_sense.tools.weather.domain.WeatherObservation
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherLogger
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import java.time.Duration
import java.time.Instant

class CalibrateBarometerFragment : AndromedaPreferenceFragment() {

    private lateinit var prefs: UserPreferences
    private lateinit var sensorService: SensorService
    private val throttle = Throttle(20)
    private val updateTimer = CoroutineTimer {
        update()
    }

    private var pressureTxt: Preference? = null
    private var seaLevelSwitch: SwitchPreferenceCompat? = null
    private var pressureSmoothingSeekBar: SeekBarPreference? = null

    private var chart: PressureChartPreference? = null

    private var history: List<WeatherObservation> = listOf()
    private var uncalibratedHistory: List<Reading<RawWeatherObservation>> = listOf()

    private lateinit var units: PressureUnits
    private val formatService by lazy { FormatService.getInstance(requireContext()) }

    private val weatherSubsystem by lazy { WeatherSubsystem.getInstance(requireContext()) }

    private val runner = CoroutineQueueRunner()

    private val logger by lazy {
        WeatherLogger(
            requireContext(),
            Duration.ofSeconds(30),
            Duration.ofMillis(500),
        )
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.barometer_calibration, rootKey)

        setIconColor(Resources.androidTextColorSecondary(requireContext()))

        prefs = UserPreferences(requireContext())
        sensorService = SensorService(requireContext())
        units = prefs.pressureUnits

        bindPreferences()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observe(weatherSubsystem.weatherChanged) {
            inBackground {
                runner.replace {
                    history = weatherSubsystem.getHistory()
                    uncalibratedHistory = weatherSubsystem.getRawHistory(true)
                    onMain {
                        updateChart()
                    }
                }
            }
        }
    }

    private fun bindPreferences() {
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

        val barometerOffsetPref = preference(R.string.pref_holder_barometer_offset)
        barometerOffsetPref?.summary = formatService.formatPressure(
            Pressure.hpa(prefs.weather.barometerOffset).convertTo(units),
            Units.getDecimalPlaces(units)
        )

        onClick(barometerOffsetPref) {
            CustomUiUtils.pickPressure(
                requireContext(),
                getString(R.string.pressure),
                getString(R.string.enter_the_current_pressure),
                default = getCurrentPressure().convertTo(units)
            ) {
                if (it == null) {
                    return@pickPressure
                }

                val currentOffset = prefs.weather.barometerOffset
                val currentReading = getCurrentPressure().pressure
                val rawReading = currentReading - currentOffset
                val newOffset = it.hpa().pressure - rawReading

                prefs.weather.barometerOffset = newOffset

                barometerOffsetPref?.summary = formatService.formatPressure(
                    Pressure.hpa(newOffset).convertTo(units),
                    Units.getDecimalPlaces(units)
                )
            }
        }

        onClick(preference(R.string.pref_reset_barometer_calibration_key)) {
            Alerts.dialog(
                requireContext(),
                getString(R.string.reset_calibration_question),
            ) {
                if (!it) {
                    prefs.weather.barometerOffset = 0f
                    barometerOffsetPref?.summary = formatService.formatPressure(
                        Pressure.hpa(prefs.weather.barometerOffset).convertTo(units),
                        Units.getDecimalPlaces(units)
                    )
                }
            }
        }

    }

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

    override fun onResume() {
        super.onResume()
        updateTimer.interval(200)
        logger.start()
    }

    override fun onPause() {
        super.onPause()
        updateTimer.stop()
        logger.stop()
    }

    private fun update() {
        // TODO: Only call this on change
        if (throttle.isThrottled()) {
            return
        }

        val pressure = getCurrentPressure()

        pressureTxt?.summary =
            formatService.formatPressure(
                pressure.convertTo(units),
                Units.getDecimalPlaces(units)
            )

        // Disable the offset preference until there's a reading
        preference(R.string.pref_holder_barometer_offset)?.isEnabled = pressure.pressure != 0f
    }

    private fun getCurrentPressure(): Pressure {
        return history.lastOrNull()?.pressure ?: Pressure.hpa(0f)
    }

}